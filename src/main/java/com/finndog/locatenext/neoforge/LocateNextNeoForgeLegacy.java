package com.finndog.locatenext.neoforge;

// Pre-1.20.5 NeoForge common entrypoint (1.20.4 only in practice). NeoForge's own 1.20.4 fork
// carries a payload API from before the modern playToServer/playToClient split — one
// registrar.play(id, reader, handler) covers both directions, keyed off a plain ResourceLocation
// id rather than a CustomPacketPayload.Type, and the handler must hop to the main thread itself
// via workHandler(). See LocateNextNeoForge for the >=1.20.5 sibling this duplicates the
// command/sync wiring of — kept as two whole files rather than one with an inner version split,
// since Stonecutter does not resolve //? markers nested inside another //? block.
//? if neoforge && <1.20.5 {

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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

@Mod(LocateNext.MOD_ID)
public final class LocateNextNeoForgeLegacy {

    public LocateNextNeoForgeLegacy(IEventBus modBus) {
        modBus.addListener(LocateNextNeoForgeLegacy::onRegisterPayloads);

        NeoForge.EVENT_BUS.addListener(LocateNextNeoForgeLegacy::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(LocateNextNeoForgeLegacy::onDatapackSync);

        LocateNext.LOGGER.info("LocateNext ready — /locatenext mod <modid>, then arrow keys.");
    }

    private static void onRegisterPayloads(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar("1");

        registrar.play(NavigatePayload.ID, NavigatePayload::read, (payload, context) ->
                context.workHandler().execute(() -> {
                    ServerPlayer player = (ServerPlayer) context.player().orElseThrow();
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
                }));

        registrar.play(SelectModPayload.ID, SelectModPayload::read, (payload, context) ->
                context.workHandler().execute(() ->
                        NavigationManager.selectMod((ServerPlayer) context.player().orElseThrow(), payload.namespace())));

        registrar.play(StructureIndexPayload.ID, StructureIndexPayload::read, (payload, context) ->
                context.workHandler().execute(() -> ClientStructureIndex.accept(payload.structures())));

        registrar.play(NavStatePayload.ID, NavStatePayload::read, (payload, context) ->
                context.workHandler().execute(() ->
                        ClientStructureIndex.acceptState(payload.namespace(), payload.index())));
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
