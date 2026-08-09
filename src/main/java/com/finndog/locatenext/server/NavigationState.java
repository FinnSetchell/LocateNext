package com.finndog.locatenext.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One player's cursor into one mod's structure list, plus their per-session search settings. */
public final class NavigationState {

    /** Vanilla {@code /locate}'s default search radius, in chunks. */
    public static final int DEFAULT_RADIUS = 100;

    private String namespace = "";
    private List<ResourceLocation> structures = List.of();
    /** -1 means "nothing visited yet", so the first `next` lands on index 0. */
    private int index = -1;

    private int radius = DEFAULT_RADIUS;
    /**
     * Vanilla's {@code skipKnownStructures}. On, the generator refuses instances that have already
     * been referenced and marks each hit as referenced on the way out — so every search returns a
     * structure nobody has been sent to before. Default on: this is a tool for looking at fresh
     * generation, and repeatedly landing on the same instance is exactly what makes that tedious.
     */
    private boolean unexploredOnly = true;
    /** Hop to the dimension a structure actually generates in instead of failing. */
    private boolean autoDimension = true;

    private final Set<ResourceLocation> visited = new HashSet<>();
    /** Per-structure instance history, so ↑/↓ can walk variations of the same structure. */
    private final Map<ResourceLocation, VariantHistory> variants = new HashMap<>();

    @Nullable private BlockPos homePos;
    @Nullable private ResourceKey<Level> homeDimension;

    public String namespace() {
        return this.namespace;
    }

    public List<ResourceLocation> structures() {
        return this.structures;
    }

    public int index() {
        return this.index;
    }

    public int size() {
        return this.structures.size();
    }

    public boolean hasSelection() {
        return !this.structures.isEmpty();
    }

    /** A fresh pick by the player: history for the previous mod is no longer wanted. */
    public void select(String namespace, List<ResourceLocation> structures) {
        this.namespace = namespace;
        this.structures = List.copyOf(structures);
        this.index = -1;
        this.visited.clear();
        this.variants.clear();
    }

    /**
     * Rebuilds the list for the namespace already selected, keeping visited marks and instance
     * history. Used after a datapack reload and when restoring a save, where the selection hasn't
     * changed but the registry contents may have.
     */
    public void reselect(List<ResourceLocation> structures) {
        this.structures = List.copyOf(structures);
        this.index = Math.min(this.index, this.structures.size() - 1);
    }

    /**
     * Fills in the structure list after a load. The saved data records only the namespace, since
     * the structures themselves come from a datapack-driven registry that may have changed between
     * sessions — so the list is rebuilt against whatever is registered now.
     */
    public void resolve(MinecraftServer server) {
        if (this.namespace.isEmpty() || !this.structures.isEmpty()) {
            return;
        }
        List<ResourceLocation> current = StructureCatalog.byNamespace(server).get(this.namespace);
        if (current == null || current.isEmpty()) {
            // The mod that was selected is no longer present.
            this.namespace = "";
            this.index = -1;
            return;
        }
        reselect(current);
    }

    public void clear() {
        this.namespace = "";
        this.structures = List.of();
        this.index = -1;
        this.visited.clear();
        this.variants.clear();
    }

    public VariantHistory variants(ResourceLocation id) {
        return this.variants.computeIfAbsent(id, key -> new VariantHistory());
    }

    /** Wraps, so holding right arrow loops the list rather than dead-ending. */
    public int step(int delta) {
        int size = this.structures.size();
        if (size == 0) {
            return -1;
        }
        // -1 + 1 -> 0 gives a clean first `next`; -1 - 1 -> size - 1 makes the first `prev` wrap
        // to the end, which is how you get at the tail of a long list quickly.
        this.index = Math.floorMod(this.index + delta, size);
        return this.index;
    }

    public boolean setIndex(int index) {
        if (index < 0 || index >= this.structures.size()) {
            return false;
        }
        this.index = index;
        return true;
    }

    @Nullable
    public ResourceLocation current() {
        return this.index >= 0 && this.index < this.structures.size() ? this.structures.get(this.index) : null;
    }

    public void markVisited(ResourceLocation id) {
        this.visited.add(id);
    }

    public boolean isVisited(ResourceLocation id) {
        return this.visited.contains(id);
    }

    public int visitedCount() {
        return this.visited.size();
    }

    public int radius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public boolean unexploredOnly() {
        return this.unexploredOnly;
    }

    public void setUnexploredOnly(boolean unexploredOnly) {
        this.unexploredOnly = unexploredOnly;
    }

    public boolean autoDimension() {
        return this.autoDimension;
    }

    public void setAutoDimension(boolean autoDimension) {
        this.autoDimension = autoDimension;
    }

    /** Recorded once, on the first teleport, so `/ln home` can undo a whole session of hopping. */
    public void rememberHome(ResourceKey<Level> dimension, BlockPos pos) {
        if (this.homePos == null) {
            this.homeDimension = dimension;
            this.homePos = pos;
        }
    }

    @Nullable
    public BlockPos homePos() {
        return this.homePos;
    }

    @Nullable
    public ResourceKey<Level> homeDimension() {
        return this.homeDimension;
    }

    public void clearHome() {
        this.homePos = null;
        this.homeDimension = null;
    }

    // ------------------------------------------------------------------ persistence

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("namespace", this.namespace);
        tag.putInt("index", this.index);
        tag.putInt("radius", this.radius);
        tag.putBoolean("unexploredOnly", this.unexploredOnly);
        tag.putBoolean("autoDimension", this.autoDimension);

        ListTag visitedList = new ListTag();
        this.visited.forEach(id -> visitedList.add(StringTag.valueOf(id.toString())));
        tag.put("visited", visitedList);

        CompoundTag variantsTag = new CompoundTag();
        this.variants.forEach((id, history) -> {
            if (!history.isEmpty()) {
                variantsTag.put(id.toString(), history.save());
            }
        });
        tag.put("variants", variantsTag);

        if (this.homePos != null && this.homeDimension != null) {
            tag.putString("homeDimension", this.homeDimension.location().toString());
            tag.putIntArray("homePos",
                    new int[]{this.homePos.getX(), this.homePos.getY(), this.homePos.getZ()});
        }
        return tag;
    }

    public static NavigationState load(CompoundTag tag) {
        NavigationState state = new NavigationState();
        state.namespace = tag.getString("namespace");
        state.index = tag.getInt("index");
        state.radius = tag.contains("radius") ? tag.getInt("radius") : DEFAULT_RADIUS;
        // contains() guards rather than getBoolean's silent false, so a tag written before these
        // existed keeps the defaults instead of flipping both off.
        state.unexploredOnly = !tag.contains("unexploredOnly") || tag.getBoolean("unexploredOnly");
        state.autoDimension = !tag.contains("autoDimension") || tag.getBoolean("autoDimension");

        ListTag visitedList = tag.getList("visited", Tag.TAG_STRING);
        for (int i = 0; i < visitedList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(visitedList.getString(i));
            if (id != null) {
                state.visited.add(id);
            }
        }

        CompoundTag variantsTag = tag.getCompound("variants");
        for (String key : variantsTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                state.variants.put(id, VariantHistory.load(variantsTag.getCompound(key)));
            }
        }

        int[] home = tag.getIntArray("homePos");
        ResourceLocation homeDimension = ResourceLocation.tryParse(tag.getString("homeDimension"));
        if (home.length == 3 && homeDimension != null) {
            state.homePos = new BlockPos(home[0], home[1], home[2]);
            state.homeDimension = ResourceKey.create(Registries.DIMENSION, homeDimension);
        }

        // structures stays empty on purpose — resolve() rebuilds it from the live registry.
        return state;
    }
}
