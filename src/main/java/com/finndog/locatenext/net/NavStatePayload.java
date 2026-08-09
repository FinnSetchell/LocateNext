package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client mirror of the cursor, so the menu can highlight where you are. */
public record NavStatePayload(String namespace, int index) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NavStatePayload> TYPE =
            new CustomPacketPayload.Type<>(LocateNext.id("nav_state"));

    public static final StreamCodec<ByteBuf, NavStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NavStatePayload::namespace,
            ByteBufCodecs.VAR_INT, NavStatePayload::index,
            NavStatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
