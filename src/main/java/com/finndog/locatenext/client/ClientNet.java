package com.finndog.locatenext.client;

import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.net.StructureIndexPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.function.Consumer;

//? if <1.20.5 {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
*///?}

/**
 * The client half of networking. Same split as {@code Net}, kept in the client package so the
 * dedicated server never loads a class referencing {@code ClientPlayNetworking}.
 */
public final class ClientNet {

    private ClientNet() {
    }

    public static void onStructureIndex(Consumer<StructureIndexPayload> handler) {
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(StructureIndexPayload.TYPE, (payload, context) ->
                handler.accept(payload));
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(StructureIndexPayload.ID,
                (client, listener, buf, sender) -> {
                    StructureIndexPayload payload = StructureIndexPayload.read(buf);
                    client.execute(() -> handler.accept(payload));
                });
        *///?}
    }

    public static void onNavState(Consumer<NavStatePayload> handler) {
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(NavStatePayload.TYPE, (payload, context) ->
                handler.accept(payload));
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(NavStatePayload.ID,
                (client, listener, buf, sender) -> {
                    NavStatePayload payload = NavStatePayload.read(buf);
                    client.execute(() -> handler.accept(payload));
                });
        *///?}
    }

    /** No-op when the server doesn't have the mod, so keybinds stay silent instead of erroring. */
    public static void send(NavigatePayload payload) {
        //? if >=1.20.5 {
        if (ClientPlayNetworking.canSend(NavigatePayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
        //?} else {
        /*if (ClientPlayNetworking.canSend(NavigatePayload.ID)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            payload.write(buf);
            ClientPlayNetworking.send(NavigatePayload.ID, buf);
        }
        *///?}
    }

    public static void send(SelectModPayload payload) {
        //? if >=1.20.5 {
        if (ClientPlayNetworking.canSend(SelectModPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
        //?} else {
        /*if (ClientPlayNetworking.canSend(SelectModPayload.ID)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            payload.write(buf);
            ClientPlayNetworking.send(SelectModPayload.ID, buf);
        }
        *///?}
    }
}
