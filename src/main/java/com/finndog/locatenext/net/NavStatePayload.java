package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
// CustomPacketPayload itself is a vanilla type that has existed since 1.20.4 (confirmed against
// the actual 1.20.4 Forge sources — it predates the 1.20.5 Type/StreamCodec rework below). Only
// 1.20.1, the one node any loader ships below 1.20.4, goes without it entirely.
//? if <1.20.4 {
/*
*///?} else {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
//?}

/** Server -> client mirror of the cursor, so the menu can highlight where you are. */
public record NavStatePayload(String namespace, int index)
        //? if <1.20.4 {
        /*
        *///?} else {
        implements CustomPacketPayload
        //?}
{

    public static final ResourceLocation ID = LocateNext.id("nav_state");

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.namespace);
        buf.writeVarInt(this.index);
    }

    public static NavStatePayload read(FriendlyByteBuf buf) {
        return new NavStatePayload(buf.readUtf(), buf.readVarInt());
    }

    // Only NeoForge's pre-1.20.5 CustomPacketPayload requires this; harmless as a plain extra
    // method everywhere else.
    public ResourceLocation id() {
        return ID;
    }

    //? if >=1.20.5 {
    public static final CustomPacketPayload.Type<NavStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, NavStatePayload> CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), NavStatePayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
