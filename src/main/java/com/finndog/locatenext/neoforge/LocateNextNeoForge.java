package com.finndog.locatenext.neoforge;

// Modern (>=1.20.5) common entrypoint. Below 1.20.5 NeoForge has a different payload API
// entirely (no CustomPacketPayload.Type/StreamCodec, no playToServer/playToClient split) — see
// LocateNextNeoForgeLegacy instead. Kept as two whole files rather than one with an inner
// version split: Stonecutter does not resolve //? markers nested inside another //? block.
//? if neoforge && >=1.20.5 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.ClientStructureIndex;
import com.finndog.locatenext.command.LocateNextCommand;
import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.Net;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.net.StructureIndexPayload;
import com.finndog.locatenext.server.NavigationManager;
import com.finndog.locatenext.server.Players;
import com.finndog.locatenext.server.StructureCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// NeoForge entrypoint (common — loaded on the dedicated server too). Payload types and their
// handlers are registered together here, unlike Fabric's two-step Net.registerTypes then
// Net.onNavigate/Net.onSelectMod — NeoForge only offers the one event for it. ClientStructureIndex
// has no client-only imports of its own, so registering its handlers from this common class is
// safe even though it lives in the client package.
@Mod(LocateNext.MOD_ID)
public final class LocateNextNeoForge {

    public LocateNextNeoForge(IEventBus modBus) {
        modBus.addListener(LocateNextNeoForge::onRegisterPayloads);

        NeoForge.EVENT_BUS.addListener(LocateNextNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(LocateNextNeoForge::onDatapackSync);

        LocateNext.LOGGER.info("LocateNext ready — /locatenext mod <modid>, then arrow keys.");
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(NavigatePayload.TYPE, NavigatePayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
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
        });

        registrar.playToServer(SelectModPayload.TYPE, SelectModPayload.CODEC, (payload, context) ->
                NavigationManager.selectMod((ServerPlayer) context.player(), payload.namespace()));

        registrar.playToClient(StructureIndexPayload.TYPE, StructureIndexPayload.CODEC,
                (payload, context) -> ClientStructureIndex.accept(payload.structures()));

        registrar.playToClient(NavStatePayload.TYPE, NavStatePayload.CODEC, (payload, context) ->
                ClientStructureIndex.acceptState(payload.namespace(), payload.index()));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        LocateNextCommand.register(event.getDispatcher());
    }

    // Fires both on a fresh player join and on a /reload, covering the same two cases Fabric
    // handles with ServerPlayConnectionEvents.JOIN and ServerLifecycleEvents.END_DATA_PACK_RELOAD.
    private static void onDatapackSync(OnDatapackSyncEvent event) {
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
        Net.send(player, new StructureIndexPayload(StructureCatalog.allIds(Players.server(player))));
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
