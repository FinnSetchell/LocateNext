package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.List;

/**
 * Every structure id the server knows, pushed on join and after each datapack reload.
 *
 * <p>The structure registry is dynamic, so the client cannot derive this itself — without the
 * push, the menu would have nothing to list.
 */
public record StructureIndexPayload(List<ResourceLocation> structures)
        //? if >=1.20.5 {
        implements CustomPacketPayload
        //?}
{

    public static final ResourceLocation ID = LocateNext.id("structure_index");

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.structures, FriendlyByteBuf::writeResourceLocation);
    }

    public static StructureIndexPayload read(FriendlyByteBuf buf) {
        return new StructureIndexPayload(buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    //? if >=1.20.5 {
    public static final CustomPacketPayload.Type<StructureIndexPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, StructureIndexPayload> CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), StructureIndexPayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
