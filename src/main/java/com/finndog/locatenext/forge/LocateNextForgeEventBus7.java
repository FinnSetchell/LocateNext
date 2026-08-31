package com.finndog.locatenext.forge;

// Forge 1.21.6+ common entrypoint (1.21.11 only, of the eight Forge nodes this mod ships).
// EventBus 7 rewrote every game-bus event (RegisterCommandsEvent and OnDatapackSyncEvent included)
// onto its own dedicated EventBus<T>, reachable as EventType.BUS — confirmed against the real
// 1.21.11 sources. MinecraftForge.EVENT_BUS still exists as a back-compat helper, but only for the
// old whole-class register(Object)/register(Class) scanning; it has no addListener(Consumer<T>)
// at all, so it cannot replace the per-event BUS fields used below. Forge's own
// SimpleChannel/ChannelBuilder networking and @Mod itself are untouched by EventBus 7. Kept as a
// whole separate file rather than an inner version split, since Stonecutter does not resolve //?
// markers nested inside another //? block.
//? if forge && >=1.21.6 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.ClientStructureIndex;
import com.finndog.locatenext.command.LocateNextCommand;
import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.net.StructureIndexPayload;
import com.finndog.locatenext.server.NavigationManager;
import com.finndog.locatenext.server.Players;
import com.finndog.locatenext.server.StructureCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

// Forge entrypoint (common — loaded on the dedicated server too). Payload registration is
// unchanged from the pre-1.21.6 sibling (LocateNextForge) — Forge's SimpleChannel API itself
// wasn't touched by EventBus 7.
@Mod(LocateNext.MOD_ID)
public final class LocateNextForgeEventBus7 {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(LocateNext.id("main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    public LocateNextForgeEventBus7() {
        registerPayloads();

        // EventBus 7 moved every game-bus event onto its own dedicated EventBus<T>, confirmed
        // against the real 1.21.11 sources — RegisterCommandsEvent and OnDatapackSyncEvent are no
        // longer reachable through MinecraftForge.EVENT_BUS#addListener at all (that back-compat
        // helper only re-exposes the old whole-class-scanning register(Object)/register(Class), not
        // single-listener registration).
        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommands);
        OnDatapackSyncEvent.BUS.addListener(this::onDatapackSync);

        LocateNext.LOGGER.info("LocateNext ready — /locatenext mod <modid>, then arrow keys.");
    }

    private static void registerPayloads() {
        int id = 0;
        CHANNEL.messageBuilder(NavigatePayload.class, id++)
                .encoder(NavigatePayload::write)
                .decoder(NavigatePayload::read)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer player = context.getSender();
                    if (player == null) {
                        return;
                    }
                    switch (payload.op()) {
                        case NavigatePayload.OP_NEXT -> NavigationManager.step(player, 1);
                        case NavigatePayload.OP_PREV -> NavigationManager.step(player, -1);
                        case NavigatePayload.OP_GOTO -> NavigationManager.goTo(player, payload.index());
                        case NavigatePayload.OP_HOME -> NavigationManager.home(player);
                        case NavigatePayload.OP_VARIANT_NEXT -> NavigationManager.variantNext(player);
                        case NavigatePayload.OP_VARIANT_PREV -> NavigationManager.variantPrev(player);
                        default -> LocateNext.LOGGER.warn("Unknown navigate op {} from {}",
                                payload.op(), player.getName().getString());
                    }
                })
                .add();

        CHANNEL.messageBuilder(SelectModPayload.class, id++)
                .encoder(SelectModPayload::write)
                .decoder(SelectModPayload::read)
                .consumerMainThread((payload, context) -> {
                    ServerPlayer player = context.getSender();
                    if (player != null) {
                        NavigationManager.selectMod(player, payload.namespace());
                    }
                })
                .add();

        CHANNEL.messageBuilder(StructureIndexPayload.class, id++)
                .encoder(StructureIndexPayload::write)
                .decoder(StructureIndexPayload::read)
                .consumerMainThread((payload, context) -> ClientStructureIndex.accept(payload.structures()))
                .add();

        CHANNEL.messageBuilder(NavStatePayload.class, id)
                .encoder(NavStatePayload::write)
                .decoder(NavStatePayload::read)
                .consumerMainThread((payload, context) -> ClientStructureIndex.acceptState(payload.namespace(), payload.index()))
                .add();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LocateNextCommand.register(event.getDispatcher());
    }

    // Fires both on a fresh player join and on a /reload, covering the same two cases Fabric
    // handles with ServerPlayConnectionEvents.JOIN and ServerLifecycleEvents.END_DATA_PACK_RELOAD.
    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ServerPlayer player = event.getPlayer();
            sendIndex(player);
            NavigationManager.syncState(player);
            return;
        }
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            sendIndex(player);
            refreshSelection(player);
        }
    }

    private static void sendIndex(ServerPlayer player) {
        com.finndog.locatenext.net.Net.send(player, new StructureIndexPayload(StructureCatalog.allIds(Players.server(player))));
    }

    // Rebuilds a player's list against the reloaded registry, keeping their position if possible.
    private static void refreshSelection(ServerPlayer player) {
        var state = NavigationManager.state(player);
        if (!state.hasSelection()) {
            return;
        }
        var structures = StructureCatalog.byNamespace(Players.server(player)).get(state.namespace());
        if (structures == null || structures.isEmpty()) {
            state.clear();
        } else {
            state.reselect(structures);
        }
        NavigationManager.markDirty(player);
        NavigationManager.syncState(player);
    }
}
*///?}
