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
// Forge sends on the same SimpleChannel its entrypoint creates, so it needs an explicit channel
// reference here exactly as Net does on the server side, and the channel lives in a different class
// per era. 1.20.1 predates the ChannelBuilder rewrite and takes the target first.
//? if forge && <1.20.4 {
/*import com.finndog.locatenext.forge.LocateNextForgeLegacy;
*///?}
//? if forge && >=1.20.4 && <1.21.6 {
/*import net.minecraftforge.network.PacketDistributor;
import com.finndog.locatenext.forge.LocateNextForge;
*///?}
//? if forge && >=1.21.6 {
/*import net.minecraftforge.network.PacketDistributor;
import com.finndog.locatenext.forge.LocateNextForgeEventBus7;
*///?}

/**
 * The client half of networking. Same split as {@code Net}, kept in the client package so the
 * dedicated server never loads a class referencing {@code ClientPlayNetworking}.
 *
 * <p>{@link #onStructureIndex} and {@link #onNavState} are Fabric only — NeoForge registers its
 * client-bound handlers alongside the server-bound ones, from the NeoForge entrypoint's single
 * {@code RegisterPayloadHandlersEvent} listener. {@link #send} covers all three loaders, since
 * {@code ClientStructureIndex} calls it directly regardless of which one is active. Every loader
 * needs a branch in both send methods: a missing one compiles to an empty body, so the GUI keeps
 * working while every click and keypress is silently dropped.
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
        //? if forge && <1.20.4 {
        /*LocateNextForgeLegacy.CHANNEL.sendToServer(payload);
        *///?}
        //? if forge && >=1.20.4 && <1.21.6 {
        /*LocateNextForge.CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
        *///?}
        //? if forge && >=1.21.6 {
        /*LocateNextForgeEventBus7.CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
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
        //? if forge && <1.20.4 {
        /*LocateNextForgeLegacy.CHANNEL.sendToServer(payload);
        *///?}
        //? if forge && >=1.20.4 && <1.21.6 {
        /*LocateNextForge.CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
        *///?}
        //? if forge && >=1.21.6 {
        /*LocateNextForgeEventBus7.CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
        *///?}
    }
}
