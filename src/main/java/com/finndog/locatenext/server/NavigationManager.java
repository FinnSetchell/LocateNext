package com.finndog.locatenext.server;

import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.Net;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns every player's navigation cursor and performs the locate + teleport.
 *
 * <p>The search runs on the server thread, exactly as vanilla {@code /locate} does. It is a
 * blocking call that can take seconds at large radii; that is a deliberate trade, because
 * {@code ChunkGenerator#findNearestMapStructure} touches the chunk source and is not safe to run
 * off-thread. Singleplayer has no tick watchdog, so a long search stalls rather than crashes.
 */
public final class NavigationManager {

    /**
     * How many times a search for a *new* instance will march its origin outward before giving up.
     * Only reached when {@code unexploredOnly} is off, since with it on the generator already
     * refuses to hand back an instance it has handed back before.
     */
    private static final int MAX_VARIANT_ATTEMPTS = 6;

    /** Fallback march distance when a structure's placement isn't a plain random spread. */
    private static final int DEFAULT_STEP_BLOCKS = 768;

    /** What a jump should do about the current structure's instance history. */
    public enum Mode {
        /** Return to the instance under the cursor, searching only if there isn't one. */
        REUSE,
        /** Step forward through history, then search for an instance never seen before. */
        NEW,
        /** Step back to the previously located instance. Never searches. */
        BACK
    }

    private NavigationManager() {
    }

    /**
     * State is read straight out of the world save, so a selection survives a restart. The
     * {@code resolve} call is a no-op except on the first access after a load, where it rebuilds
     * the structure list against the registry as it is now.
     */
    public static NavigationState state(ServerPlayer player) {
        NavigationState state = LocateNextSavedData.get(Players.server(player)).state(player.getUUID());
        state.resolve(Players.server(player));
        return state;
    }

    /**
     * Queues the world save. Called at the end of every action that changes a cursor, a history or
     * a setting — SavedData only writes when something has marked it, and a missed call means work
     * silently lost on restart.
     */
    public static void markDirty(ServerPlayer player) {
        LocateNextSavedData.get(Players.server(player)).setDirty();
    }

    // ------------------------------------------------------------------ selection

    /** @return false if the namespace has no structures (message already sent). */
    public static boolean selectMod(ServerPlayer player, String namespace) {
        Map<String, List<ResourceLocation>> catalog = StructureCatalog.byNamespace(Players.server(player));
        List<ResourceLocation> structures = catalog.get(namespace);
        if (structures == null || structures.isEmpty()) {
            Msg.error(player, "No structures registered under '" + namespace + "'.");
            Msg.info(player, Component.empty()
                    .append(Msg.dim("Try "))
                    .append(Msg.button("/locatenext mods", "/locatenext mods", ChatFormatting.AQUA)));
            return false;
        }

        NavigationState state = state(player);
        state.select(namespace, structures);
        markDirty(player);
        syncState(player);

        Msg.info(player, Component.empty()
                .append(Msg.dim("Selected "))
                .append(Msg.structure(namespace))
                .append(Msg.dim(" — "))
                .append(Msg.value(String.valueOf(structures.size())))
                .append(Msg.dim(" structures. Press "))
                .append(Component.literal("→").withStyle(ChatFormatting.GOLD))
                .append(Msg.dim(" to start.")));
        Msg.plain(player, Msg.navBar());
        return true;
    }

    public static void listMods(ServerPlayer player) {
        Map<String, List<ResourceLocation>> catalog = StructureCatalog.byNamespace(Players.server(player));
        Msg.info(player, Msg.dim(catalog.size() + " namespaces with structures:"));
        catalog.forEach((namespace, structures) -> Msg.plain(player, Component.empty()
                .append(Msg.button("  " + namespace, "/locatenext mod " + namespace, ChatFormatting.YELLOW))
                .append(Msg.dim(" (" + structures.size() + ")"))));
    }

    public static void listStructures(ServerPlayer player) {
        NavigationState state = state(player);
        if (!state.hasSelection()) {
            Msg.error(player, "No mod selected. Use /locatenext mod <modid>.");
            return;
        }
        Msg.info(player, Component.empty()
                .append(Msg.structure(state.namespace()))
                .append(Msg.dim(" — " + state.size() + " structures, " + state.visitedCount() + " visited")));
        List<ResourceLocation> structures = state.structures();
        for (int i = 0; i < structures.size(); i++) {
            ResourceLocation id = structures.get(i);
            boolean current = i == state.index();
            ChatFormatting colour = current ? ChatFormatting.GREEN
                    : state.isVisited(id) ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW;
            int found = state.variants(id).size();
            Msg.plain(player, Component.empty()
                    .append(Msg.dim(String.format(" %s%2d. ", current ? "▶" : " ", i + 1)))
                    .append(Msg.button(id.getPath(), "/locatenext goto " + (i + 1), colour))
                    .append(found > 1 ? Msg.dim("  ×" + found) : Component.empty()));
        }
    }

    /** The instance history for whichever structure the cursor is on. */
    public static void listVariants(ServerPlayer player) {
        NavigationState state = state(player);
        ResourceLocation id = state.current();
        if (id == null) {
            Msg.error(player, "No structure selected yet.");
            return;
        }
        VariantHistory history = state.variants(id);
        if (history.isEmpty()) {
            Msg.info(player, Msg.dim("No instances of " + id + " located yet."));
            return;
        }
        Msg.info(player, Component.empty()
                .append(Msg.structure(id.toString()))
                .append(Msg.dim(" — " + history.size() + " instance(s) found")));
        List<VariantHistory.Landing> landings = history.landings();
        for (int i = 0; i < landings.size(); i++) {
            VariantHistory.Landing landing = landings.get(i);
            boolean current = i + 1 == history.position();
            Msg.plain(player, Component.empty()
                    .append(Msg.dim(String.format(" %s%2d. ", current ? "▶" : " ", i + 1)))
                    .append(Msg.coords(landing.landing()))
                    .append(Msg.dim("  " + LocateNext.keyId(landing.dimension()).getPath())));
        }
    }

    public static void status(ServerPlayer player) {
        NavigationState state = state(player);
        if (!state.hasSelection()) {
            Msg.info(player, Msg.dim("No mod selected."));
            return;
        }
        ResourceLocation current = state.current();
        Msg.info(player, Component.empty()
                .append(Msg.structure(state.namespace()))
                .append(Msg.dim("  "))
                .append(Msg.value((state.index() + 1) + "/" + state.size()))
                .append(Msg.dim(current == null ? "  (not started)" : "  " + current.getPath())));
        Msg.info(player, Msg.dim("radius " + state.radius() + " chunks"
                + " | fresh-only " + (state.unexploredOnly() ? "on" : "off")
                + " | auto-dimension " + (state.autoDimension() ? "on" : "off")));
    }

    // ------------------------------------------------------------------ navigation

    public static void step(ServerPlayer player, int delta) {
        NavigationState state = state(player);
        if (!requireSelection(player, state)) {
            return;
        }
        state.step(delta);
        syncState(player);
        jump(player, state, Mode.REUSE);
    }

    /** ↑ — a different instance of the structure you're already on. */
    public static void variantNext(ServerPlayer player) {
        NavigationState state = state(player);
        if (!requireSelection(player, state)) {
            return;
        }
        if (state.current() == null) {
            state.step(1);
            syncState(player);
            jump(player, state, Mode.REUSE);
            return;
        }
        jump(player, state, Mode.NEW);
    }

    /** ↓ — back to the instance of this structure you were at before. */
    public static void variantPrev(ServerPlayer player) {
        NavigationState state = state(player);
        if (!requireSelection(player, state) || state.current() == null) {
            return;
        }
        jump(player, state, Mode.BACK);
    }

    /** @param index zero-based. */
    public static void goTo(ServerPlayer player, int index) {
        NavigationState state = state(player);
        if (!requireSelection(player, state)) {
            return;
        }
        if (!state.setIndex(index)) {
            Msg.error(player, "Index out of range (1-" + state.size() + ").");
            return;
        }
        syncState(player);
        jump(player, state, Mode.REUSE);
    }

    public static void home(ServerPlayer player) {
        NavigationState state = state(player);
        BlockPos home = state.homePos();
        ResourceKey<Level> dimension = state.homeDimension();
        if (home == null || dimension == null) {
            Msg.error(player, "No saved position — it's recorded on your first jump.");
            return;
        }
        ServerLevel level = Players.server(player).getLevel(dimension);
        if (level == null) {
            Msg.error(player, "Saved dimension " + LocateNext.keyId(dimension) + " is no longer loaded.");
            return;
        }
        teleport(player, level, home);
        Msg.info(player, Component.empty()
                .append(Msg.dim("Returned to "))
                .append(Msg.coords(home))
                .append(Msg.dim(" in " + LocateNext.keyId(dimension).getPath())));
    }

    private static boolean requireSelection(ServerPlayer player, NavigationState state) {
        if (state.hasSelection()) {
            return true;
        }
        Msg.error(player, "No mod selected. Use /locatenext mod <modid> or open the menu.");
        return false;
    }

    // ------------------------------------------------------------------ the jump

    /**
     * Single choke point for persistence: every jump can move a cursor, extend a history or record
     * a home position, and {@link #perform} has too many early returns to mark each one safely.
     */
    private static void jump(ServerPlayer player, NavigationState state, Mode mode) {
        try {
            perform(player, state, mode);
        } finally {
            markDirty(player);
        }
    }

    private static void perform(ServerPlayer player, NavigationState state, Mode mode) {
        ResourceLocation id = state.current();
        if (id == null) {
            return;
        }
        VariantHistory history = state.variants(id);

        switch (mode) {
            case BACK -> {
                VariantHistory.Landing landing = history.back();
                if (landing == null) {
                    reportHeader(player, state, id, history);
                    Msg.info(player, Msg.dim("  already at the first instance found — press ")
                            .append(Component.literal("↑").withStyle(ChatFormatting.GOLD))
                            .append(Msg.dim(" for a new one")));
                    return;
                }
                revisit(player, state, id, history, landing);
                return;
            }
            case REUSE -> {
                VariantHistory.Landing landing = history.current();
                if (landing != null) {
                    revisit(player, state, id, history, landing);
                    return;
                }
            }
            case NEW -> {
                // Forward through instances already found before spending a search on a new one.
                VariantHistory.Landing landing = history.forward();
                if (landing != null) {
                    revisit(player, state, id, history, landing);
                    return;
                }
            }
        }

        search(player, state, id, history, mode == Mode.NEW);
    }

    /** Teleport to an instance already in the history — no search, so it's instant. */
    private static void revisit(ServerPlayer player, NavigationState state, ResourceLocation id,
                                VariantHistory history, VariantHistory.Landing landing) {
        ServerLevel level = Players.server(player).getLevel(landing.dimension());
        if (level == null) {
            Msg.error(player, "Dimension " + LocateNext.keyId(landing.dimension()) + " is no longer loaded.");
            return;
        }
        state.rememberHome(Players.level(player).dimension(), player.blockPosition());
        teleport(player, level, landing.landing());
        state.markVisited(id);

        reportHeader(player, state, id, history);
        Msg.info(player, Component.empty()
                .append(Msg.dim("  at ")).append(Msg.coords(landing.landing()))
                .append(Msg.dim("  revisited, no search")));
        Msg.plain(player, Msg.navBar());
    }

    private static void search(ServerPlayer player, NavigationState state, ResourceLocation id,
                               VariantHistory history, boolean wantNew) {
        MinecraftServer server = Players.server(player);
        Registry<Structure> registry = StructureCatalog.registry(server);
        Optional<Holder.Reference<Structure>> holder =
                StructureCatalog.holder(registry, ResourceKey.create(Registries.STRUCTURE, id));
        if (holder.isEmpty()) {
            Msg.error(player, "Structure " + id + " vanished from the registry (datapack reload?).");
            return;
        }

        ServerLevel origin = Players.level(player);
        Optional<ServerLevel> target = state.autoDimension()
                ? StructureCatalog.findLevelFor(server, origin, holder.get().value())
                : StructureCatalog.canGenerateIn(origin, holder.get().value())
                    ? Optional.of(origin) : Optional.empty();

        if (target.isEmpty()) {
            reportHeader(player, state, id, history);
            Msg.info(player, Msg.dim("  cannot generate in ")
                    .append(Component.literal(LocateNext.keyId(origin.dimension()).toString())
                            .withStyle(ChatFormatting.RED))
                    .append(Msg.dim(state.autoDimension()
                            ? " — or any other loaded dimension" : " (auto-dimension is off)")));
            Msg.plain(player, Msg.navBar());
            return;
        }

        ServerLevel level = target.get();
        BlockPos playerPos = player.blockPosition();
        BlockPos playerInTarget = scaleAcrossDimensions(playerPos, origin, level);

        // Hunting a new instance starts from the last one found, so the search marches away from
        // it rather than repeatedly rediscovering whatever is nearest to the player.
        VariantHistory.Landing last = history.current();
        BlockPos base = wantNew && last != null && last.dimension() == level.dimension()
                ? last.structurePos()
                : playerInTarget;

        // Sent before the blocking search, not after. Packets are handed to the netty event loop
        // rather than written on the server thread, so this reaches the client while the search is
        // still running — which is the whole point, given a cold search can take seconds. Only the
        // searching paths say it; a revisit is instant and would just be noise.
        Msg.info(player, Msg.dim("Locating " + id + "…"));

        int step = stepBlocks(level, holder.get());
        Pair<BlockPos, Holder<Structure>> hit = null;
        boolean sawAnything = false;
        int attempts = 0;
        long startNanos = System.nanoTime();

        for (; attempts < (wantNew ? MAX_VARIANT_ATTEMPTS : 1); attempts++) {
            BlockPos from = marchOutward(base, attempts, step);
            Pair<BlockPos, Holder<Structure>> candidate = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, HolderSet.direct(holder.get()), from,
                            state.radius(), state.unexploredOnly());
            if (candidate == null) {
                continue;
            }
            sawAnything = true;
            if (!history.contains(level.dimension(), candidate.getFirst())) {
                hit = candidate;
                break;
            }
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        if (hit == null) {
            reportHeader(player, state, id, history);
            Msg.info(player, Component.empty()
                    .append(Msg.dim("  "))
                    .append(Component.literal(sawAnything
                                    ? "no instance found that you haven't already visited"
                                    : "not found within " + state.radius() + " chunks")
                            .withStyle(ChatFormatting.RED))
                    .append(Msg.dim("  (" + elapsedMs + " ms, " + attempts + " search"
                            + (attempts == 1 ? ")" : "es)"))));
            Msg.plain(player, Msg.navBar());
            return;
        }

        BlockPos structurePos = hit.getFirst();
        BlockPos landing = SafeSpot.find(level, structurePos);
        double distance = Math.sqrt(playerInTarget.distSqr(
                new BlockPos(structurePos.getX(), playerInTarget.getY(), structurePos.getZ())));

        history.add(new VariantHistory.Landing(level.dimension(), structurePos, landing));
        state.rememberHome(origin.dimension(), playerPos);
        teleport(player, level, landing);
        state.markVisited(id);

        // Only now, so the header's variant counter reflects the instance just added.
        reportHeader(player, state, id, history);

        MutableComponent line = Component.empty()
                .append(Msg.dim("  at ")).append(Msg.coords(landing))
                .append(Msg.dim("  "))
                .append(Component.literal(Msg.formatDistance(distance) + " blocks "
                                + Msg.compass(playerInTarget, structurePos))
                        .withStyle(ChatFormatting.GREEN))
                .append(Msg.dim("  in " + elapsedMs + " ms"));
        if (attempts > 0) {
            line.append(Msg.dim(" over " + (attempts + 1) + " searches"));
        }
        if (level != origin) {
            line.append(Msg.dim("  → " + LocateNext.keyId(level.dimension()).getPath()));
        }
        Msg.info(player, line);
        Msg.plain(player, Msg.navBar());
    }

    private static void reportHeader(ServerPlayer player, NavigationState state,
                                     ResourceLocation id, VariantHistory history) {
        MutableComponent header = Component.empty()
                .append(Msg.value((state.index() + 1) + "/" + state.size()))
                .append(Msg.dim("  "))
                .append(Msg.structure(id.toString()));
        if (history.size() > 1) {
            header.append(Msg.dim("  variant "))
                    .append(Component.literal(history.position() + "/" + history.size())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        Msg.info(player, header);
    }

    // ------------------------------------------------------------------ sweep

    /**
     * Locates every structure in the selected mod without teleporting, and reports which ones the
     * generator could not place. Catches "this structure never spawns" bugs in one pass.
     */
    public static void sweep(ServerPlayer player) {
        NavigationState state = state(player);
        if (!requireSelection(player, state)) {
            return;
        }

        MinecraftServer server = Players.server(player);
        Registry<Structure> registry = StructureCatalog.registry(server);
        ServerLevel origin = Players.level(player);
        BlockPos playerPos = player.blockPosition();

        Msg.info(player, Msg.dim("Sweeping " + state.size() + " structures at radius "
                + state.radius() + " — the server will hang until this finishes."));

        List<String> missing = new ArrayList<>();
        int found = 0;
        long sweepStart = System.nanoTime();

        for (ResourceLocation id : state.structures()) {
            Optional<Holder.Reference<Structure>> holder =
                    StructureCatalog.holder(registry, ResourceKey.create(Registries.STRUCTURE, id));
            if (holder.isEmpty()) {
                missing.add(id.getPath() + " (not in registry)");
                continue;
            }

            Optional<ServerLevel> target = StructureCatalog.findLevelFor(server, origin, holder.get().value());
            if (target.isEmpty()) {
                missing.add(id.getPath() + " (no dimension accepts its biomes)");
                continue;
            }

            ServerLevel level = target.get();
            BlockPos searchFrom = scaleAcrossDimensions(playerPos, origin, level);
            long start = System.nanoTime();
            // Always false here: a sweep is a survey, and marking every structure in the world as
            // referenced as a side effect of surveying it would poison later navigation.
            Pair<BlockPos, Holder<Structure>> hit = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, HolderSet.direct(holder.get()), searchFrom,
                            state.radius(), false);
            long ms = (System.nanoTime() - start) / 1_000_000L;

            if (hit == null) {
                missing.add(id.getPath() + " (not within " + state.radius() + " chunks, " + ms + " ms)");
            } else {
                found++;
                double distance = Math.sqrt(searchFrom.distSqr(
                        new BlockPos(hit.getFirst().getX(), searchFrom.getY(), hit.getFirst().getZ())));
                Msg.plain(player, Component.empty()
                        .append(Component.literal("  ✔ ").withStyle(ChatFormatting.GREEN))
                        .append(Msg.structure(id.getPath()))
                        .append(Msg.dim("  " + Msg.formatDistance(distance) + " blocks, " + ms + " ms"
                                + (level != origin ? ", " + LocateNext.keyId(level.dimension()).getPath() : ""))));
            }
        }

        long totalMs = (System.nanoTime() - sweepStart) / 1_000_000L;
        for (String entry : missing) {
            Msg.plain(player, Component.empty()
                    .append(Component.literal("  ✘ ").withStyle(ChatFormatting.RED))
                    .append(Msg.dim(entry)));
        }
        Msg.info(player, Component.empty()
                .append(Component.literal(found + " found").withStyle(ChatFormatting.GREEN))
                .append(Msg.dim(", "))
                .append(Component.literal(missing.size() + " missing").withStyle(
                        missing.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Msg.dim("  in " + totalMs + " ms")));
    }

    // ------------------------------------------------------------------ helpers

    /**
     * How far to move the search origin per retry. Sized from the structure's own placement grid,
     * so one step lands in the neighbouring cell and the nearest hit from there is a different
     * instance.
     */
    private static int stepBlocks(ServerLevel level, Holder<Structure> holder) {
        for (StructurePlacement placement : level.getChunkSource().getGeneratorState()
                .getPlacementsForStructure(holder)) {
            if (placement instanceof RandomSpreadStructurePlacement spread) {
                return Math.max(spread.spacing(), 4) * 16 * 3 / 2;
            }
        }
        return DEFAULT_STEP_BLOCKS;
    }

    /** Attempt 0 is the base itself; later attempts spiral out through the eight compass points. */
    private static BlockPos marchOutward(BlockPos base, int attempt, int step) {
        if (attempt == 0) {
            return base;
        }
        int i = attempt - 1;
        int ring = i / 8 + 1;
        double angle = (i % 8) * Math.PI / 4.0;
        int dx = (int) Math.round(Math.sin(angle) * step * ring);
        int dz = (int) Math.round(-Math.cos(angle) * step * ring);
        return base.offset(dx, 0, dz);
    }

    /**
     * Nether coordinates are 1:8 with the Overworld, so searching from the raw player position
     * after an auto-dimension hop would start thousands of blocks off.
     */
    private static BlockPos scaleAcrossDimensions(BlockPos pos, ServerLevel from, ServerLevel to) {
        if (from == to) {
            return pos;
        }
        double scale = DimensionType.getTeleportationScale(from.dimensionType(), to.dimensionType());
        return BlockPos.containing(pos.getX() * scale, pos.getY(), pos.getZ() * scale);
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.fallDistance = 0.0F;
        // 1.21.2 folded the relative-movement set and a camera flag into the signature.
        //? if >=1.21.2 {
        /*player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        *///?} else {
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        //?}
        player.fallDistance = 0.0F;
    }

    public static void syncState(ServerPlayer player) {
        NavigationState state = state(player);
        Net.send(player, new NavStatePayload(state.namespace(), state.index()));
    }

}
