package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Every structure id the server knows, pushed on join and after each datapack reload.
 *
 * <p>The structure registry is dynamic, so the client cannot derive this itself — without the
 * push, the menu would have nothing to list.
 */
public record StructureIndexPayload(List<ResourceLocation> structures) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureIndexPayload> TYPE =
            new CustomPacketPayload.Type<>(LocateNext.id("structure_index"));

    public static final StreamCodec<ByteBuf, StructureIndexPayload> CODEC =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(StructureIndexPayload::new, StructureIndexPayload::structures);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
