package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: pick a namespace from the menu, equivalent to {@code /locatenext mod <id>}. */
public record SelectModPayload(String namespace) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectModPayload> TYPE =
            new CustomPacketPayload.Type<>(LocateNext.id("select_mod"));

    public static final StreamCodec<ByteBuf, SelectModPayload> CODEC =
            ByteBufCodecs.STRING_UTF8.map(SelectModPayload::new, SelectModPayload::namespace);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
