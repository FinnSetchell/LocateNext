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

/**
 * Client -> server navigation request. One payload for every action so a new keybind or menu
 * button never needs a new packet type.
 *
 * <p>{@link #write} and {@link #read} are the whole wire format and are version-agnostic. 1.20.5
 * introduced {@code CustomPacketPayload}; below it, {@code Net} sends these buffers directly.
 *
 * @param op    one of the {@code OP_*} constants
 * @param index zero-based target, only read for {@link #OP_GOTO}
 */
public record NavigatePayload(int op, int index)
        //? if <1.20.4 {
        /*
        *///?} else {
        implements CustomPacketPayload
        //?}
{

    public static final int OP_NEXT = 0;
    public static final int OP_PREV = 1;
    public static final int OP_GOTO = 2;
    public static final int OP_HOME = 3;
    /** A different instance of the structure already under the cursor. */
    public static final int OP_VARIANT_NEXT = 4;
    /** Back to the instance of that structure visited before this one. */
    public static final int OP_VARIANT_PREV = 5;

    public static final ResourceLocation ID = LocateNext.id("navigate");

    public static NavigatePayload of(int op) {
        return new NavigatePayload(op, 0);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.op);
        buf.writeVarInt(this.index);
    }

    public static NavigatePayload read(FriendlyByteBuf buf) {
        return new NavigatePayload(buf.readVarInt(), buf.readVarInt());
    }

    // Only NeoForge's pre-1.20.5 CustomPacketPayload requires this; harmless as a plain extra
    // method everywhere else.
    public ResourceLocation id() {
        return ID;
    }

    //? if >=1.20.5 {
    public static final CustomPacketPayload.Type<NavigatePayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, NavigatePayload> CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), NavigatePayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
