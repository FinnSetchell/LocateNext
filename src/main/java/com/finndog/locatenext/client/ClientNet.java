package com.finndog.locatenext.client;

import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.net.StructureIndexPayload;

import java.util.function.Consumer;

//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//?}
//? if fabric && <1.20.5 {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
*///?}
// NeoForge moved the client-to-server send from PacketDistributor to a dedicated class at
// 1.21.11; both are covered so send() keeps the same call shape either side of that boundary.
//? if neoforge && <1.21.11 {
/*import net.neoforged.neoforge.network.PacketDistributor;
*///?}
//? if neoforge && >=1.21.11 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}

/**
 * The client half of networking. Same split as {@code Net}, kept in the client package so the
 * dedicated server never loads a class referencing {@code ClientPlayNetworking}.
 *
 * <p>{@link #onStructureIndex} and {@link #onNavState} are Fabric only — NeoForge registers its
 * client-bound handlers alongside the server-bound ones, from the NeoForge entrypoint's single
 * {@code RegisterPayloadHandlersEvent} listener. {@link #send} covers both loaders, since
 * {@code ClientStructureIndex} calls it directly regardless of which one is active.
 */
public final class ClientNet {

    private ClientNet() {
    }

    /** Fabric only. */
    public static void onStructureIndex(Consumer<StructureIndexPayload> handler) {
        //? if fabric && >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(StructureIndexPayload.TYPE, (payload, context) ->
                handler.accept(payload));
        //?}
        //? if fabric && <1.20.5 {
        /*ClientPlayNetworking.registerGlobalReceiver(StructureIndexPayload.ID,
                (client, listener, buf, sender) -> {
                    StructureIndexPayload payload = StructureIndexPayload.read(buf);
                    client.execute(() -> handler.accept(payload));
                });
        *///?}
    }

    /** Fabric only. */
    public static void onNavState(Consumer<NavStatePayload> handler) {
        //? if fabric && >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(NavStatePayload.TYPE, (payload, context) ->
                handler.accept(payload));
        //?}
        //? if fabric && <1.20.5 {
        /*ClientPlayNetworking.registerGlobalReceiver(NavStatePayload.ID,
                (client, listener, buf, sender) -> {
                    NavStatePayload payload = NavStatePayload.read(buf);
                    client.execute(() -> handler.accept(payload));
                });
        *///?}
    }

    /** No-op when the server doesn't have the mod, so keybinds stay silent instead of erroring. */
    public static void send(NavigatePayload payload) {
        //? if fabric && >=1.20.5 {
        if (ClientPlayNetworking.canSend(NavigatePayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
        //?}
        //? if fabric && <1.20.5 {
        /*if (ClientPlayNetworking.canSend(NavigatePayload.ID)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            payload.write(buf);
            ClientPlayNetworking.send(NavigatePayload.ID, buf);
        }
        *///?}
        //? if neoforge && >=1.20.5 && <1.21.11 {
        /*PacketDistributor.sendToServer(payload);
        *///?}
        //? if neoforge && <1.20.5 {
        /*PacketDistributor.SERVER.noArg().send(payload);
        *///?}
        //? if neoforge && >=1.21.11 {
        /*ClientPacketDistributor.sendToServer(payload);
        *///?}
    }

    public static void send(SelectModPayload payload) {
        //? if fabric && >=1.20.5 {
        if (ClientPlayNetworking.canSend(SelectModPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
        //?}
        //? if fabric && <1.20.5 {
        /*if (ClientPlayNetworking.canSend(SelectModPayload.ID)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            payload.write(buf);
            ClientPlayNetworking.send(SelectModPayload.ID, buf);
        }
        *///?}
        //? if neoforge && >=1.20.5 && <1.21.11 {
        /*PacketDistributor.sendToServer(payload);
        *///?}
        //? if neoforge && <1.20.5 {
        /*PacketDistributor.SERVER.noArg().send(payload);
        *///?}
        //? if neoforge && >=1.21.11 {
        /*ClientPacketDistributor.sendToServer(payload);
        *///?}
    }
}
