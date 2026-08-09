package com.finndog.locatenext.fabric;

import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.command.LocateNextCommand;
import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.net.StructureIndexPayload;
import com.finndog.locatenext.server.NavigationManager;
import com.finndog.locatenext.server.StructureCatalog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class LocateNextFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // PayloadTypeRegistry is common to both sides, so registering here covers the client too.
        PayloadTypeRegistry.playS2C().register(StructureIndexPayload.TYPE, StructureIndexPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NavStatePayload.TYPE, NavStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NavigatePayload.TYPE, NavigatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectModPayload.TYPE, SelectModPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(NavigatePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            switch (payload.op()) {
                case NavigatePayload.OP_NEXT -> NavigationManager.step(player, 1);
                case NavigatePayload.OP_PREV -> NavigationManager.step(player, -1);
                case NavigatePayload.OP_GOTO -> NavigationManager.goTo(player, payload.index());
                case NavigatePayload.OP_HOME -> NavigationManager.home(player);
                case NavigatePayload.OP_VARIANT_NEXT -> NavigationManager.variantNext(player);
                case NavigatePayload.OP_VARIANT_PREV -> NavigationManager.variantPrev(player);
                default -> LocateNext.LOGGER.warn("Unknown navigate op {} from {}",
                        payload.op(), player.getGameProfile().getName());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(SelectModPayload.TYPE, (payload, context) ->
                NavigationManager.selectMod(context.player(), payload.namespace()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                LocateNextCommand.register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendIndex(server, handler.player);
            NavigationManager.syncState(handler.player);
        });

        // Datapacks define structures, so a /reload can add or remove entries the client is
        // showing. Re-push rather than let the menu drift out of sync.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendIndex(server, player);
                refreshSelection(player);
            }
        });

        // No SERVER_STOPPED cleanup: state lives in the world save now, and dropping it on stop
        // would defeat the point of saving it.

        LocateNext.LOGGER.info("LocateNext ready — /locatenext mod <modid>, then arrow keys.");
    }

    private static void sendIndex(MinecraftServer server, ServerPlayer player) {
        ServerPlayNetworking.send(player, new StructureIndexPayload(StructureCatalog.allIds(server)));
    }

    /** Rebuilds a player's list against the reloaded registry, keeping their position if possible. */
    private static void refreshSelection(ServerPlayer player) {
        var state = NavigationManager.state(player);
        if (!state.hasSelection()) {
            return;
        }
        var structures = StructureCatalog.byNamespace(player.server).get(state.namespace());
        if (structures == null || structures.isEmpty()) {
            state.clear();
        } else {
            // reselect, not select: a reload changes what's registered, not where the player has
            // already been, so visited marks and instance history are kept.
            state.reselect(structures);
        }
        NavigationManager.markDirty(player);
        NavigationManager.syncState(player);
    }
}
