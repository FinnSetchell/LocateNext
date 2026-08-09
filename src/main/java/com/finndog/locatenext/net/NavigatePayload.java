package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server navigation request. One payload for every action so a new keybind or menu
 * button never needs a new packet type.
 *
 * @param op    one of the {@code OP_*} constants
 * @param index zero-based target, only read for {@link #OP_GOTO}
 */
public record NavigatePayload(int op, int index) implements CustomPacketPayload {

    public static final int OP_NEXT = 0;
    public static final int OP_PREV = 1;
    public static final int OP_GOTO = 2;
    public static final int OP_HOME = 3;
    /** A different instance of the structure already under the cursor. */
    public static final int OP_VARIANT_NEXT = 4;
    /** Back to the instance of that structure visited before this one. */
    public static final int OP_VARIANT_PREV = 5;

    public static final CustomPacketPayload.Type<NavigatePayload> TYPE =
            new CustomPacketPayload.Type<>(LocateNext.id("navigate"));

    public static final StreamCodec<ByteBuf, NavigatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NavigatePayload::op,
            ByteBufCodecs.VAR_INT, NavigatePayload::index,
            NavigatePayload::new);

    public static NavigatePayload of(int op) {
        return new NavigatePayload(op, 0);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
