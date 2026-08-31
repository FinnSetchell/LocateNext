package com.finndog.locatenext.net;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

//? if fabric {
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//?}
//? if fabric && >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}
//? if fabric && <1.20.5 {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
*///?}
//? if neoforge {
/*import net.neoforged.neoforge.network.PacketDistributor;
*///?}

/**
 * The server half of networking, with the two Fabric eras and NeoForge behind one set of calls.
 *
 * <p>1.20.5 replaced channel-plus-buffer with typed {@code CustomPacketPayload}s and a codec
 * registry. The wire format is identical either way — each payload's own {@code write}/{@code read}
 * is the whole of it — so only registration and sending differ, and they differ here rather than
 * at every call site. The methods are deliberately one-per-payload rather than generic: bridging a
 * channel API and a typed API through one generic signature needs casts and a channel-to-type
 * lookup that buy nothing.
 *
 * <p>One behavioural difference is load-bearing: the old Fabric API invokes handlers on the netty
 * thread, where the new one already dispatches to the main thread. The handlers below therefore hop
 * to the server thread explicitly on the old path — touching player or world state from netty would
 * be a race that only shows up under load.
 *
 * <p>NeoForge has no equivalent of {@link #registerTypes}, {@link #onNavigate} or
 * {@link #onSelectMod} — it registers payload types and handlers together in one step, from the
 * NeoForge entrypoint's {@code RegisterPayloadHandlersEvent} listener. Those three methods are
 * therefore Fabric-only; only {@link #send} has a NeoForge body, since sending is the one call this
 * class needs to make on behalf of loader-agnostic code such as {@code NavigationManager}.
 */
public final class Net {

    private Net() {
    }

    /** Fabric only — called once on both sides during mod init, before any receiver is registered. */
    public static void registerTypes() {
        // 26.1 renamed these to match vanilla's clientbound/serverbound wording.
        //? if fabric && >=26.1 {
        /*PayloadTypeRegistry.clientboundPlay().register(StructureIndexPayload.TYPE, StructureIndexPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NavStatePayload.TYPE, NavStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NavigatePayload.TYPE, NavigatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SelectModPayload.TYPE, SelectModPayload.CODEC);
        *///?}
        //? if fabric && >=1.20.5 && <26.1 {
        PayloadTypeRegistry.playS2C().register(StructureIndexPayload.TYPE, StructureIndexPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NavStatePayload.TYPE, NavStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NavigatePayload.TYPE, NavigatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectModPayload.TYPE, SelectModPayload.CODEC);
        //?}
        // Below 1.20.5 on Fabric: nothing to declare — the old API keys packets by channel id at
        // receiver registration. On NeoForge: nothing to declare here at all — see the class doc.
    }

    /** Fabric only. */
    public static void onNavigate(BiConsumer<ServerPlayer, NavigatePayload> handler) {
        //? if fabric && >=1.20.5 {
        ServerPlayNetworking.registerGlobalReceiver(NavigatePayload.TYPE, (payload, context) ->
                handler.accept(context.player(), payload));
        //?}
        //? if fabric && <1.20.5 {
        /*ServerPlayNetworking.registerGlobalReceiver(NavigatePayload.ID,
                (server, player, listener, buf, sender) -> {
                    NavigatePayload payload = NavigatePayload.read(buf);
                    server.execute(() -> handler.accept(player, payload));
                });
        *///?}
    }

    /** Fabric only. */
    public static void onSelectMod(BiConsumer<ServerPlayer, SelectModPayload> handler) {
        //? if fabric && >=1.20.5 {
        ServerPlayNetworking.registerGlobalReceiver(SelectModPayload.TYPE, (payload, context) ->
                handler.accept(context.player(), payload));
        //?}
        //? if fabric && <1.20.5 {
        /*ServerPlayNetworking.registerGlobalReceiver(SelectModPayload.ID,
                (server, player, listener, buf, sender) -> {
                    SelectModPayload payload = SelectModPayload.read(buf);
                    server.execute(() -> handler.accept(player, payload));
                });
        *///?}
    }

    public static void send(ServerPlayer player, StructureIndexPayload payload) {
        //? if fabric && >=1.20.5 {
        ServerPlayNetworking.send(player, payload);
        //?}
        //? if fabric && <1.20.5 {
        /*FriendlyByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, StructureIndexPayload.ID, buf);
        *///?}
        //? if neoforge && >=1.20.5 {
        /*PacketDistributor.sendToPlayer(player, payload);
        *///?}
        //? if neoforge && <1.20.5 {
        /*PacketDistributor.PLAYER.with(player).send(payload);
        *///?}
    }

    public static void send(ServerPlayer player, NavStatePayload payload) {
        //? if fabric && >=1.20.5 {
        ServerPlayNetworking.send(player, payload);
        //?}
        //? if fabric && <1.20.5 {
        /*FriendlyByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, NavStatePayload.ID, buf);
        *///?}
        //? if neoforge && >=1.20.5 {
        /*PacketDistributor.sendToPlayer(player, payload);
        *///?}
        //? if neoforge && <1.20.5 {
        /*PacketDistributor.PLAYER.with(player).send(payload);
        *///?}
    }
}
