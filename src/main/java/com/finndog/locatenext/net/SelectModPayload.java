package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/** Client -> server: pick a namespace from the menu, equivalent to {@code /locatenext mod <id>}. */
public record SelectModPayload(String namespace)
        //? if >=1.20.5 {
        implements CustomPacketPayload
        //?}
{

    public static final ResourceLocation ID = LocateNext.id("select_mod");

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.namespace);
    }

    public static SelectModPayload read(FriendlyByteBuf buf) {
        return new SelectModPayload(buf.readUtf());
    }

    //? if >=1.20.5 {
    public static final CustomPacketPayload.Type<SelectModPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SelectModPayload> CODEC =
            StreamCodec.of((buf, payload) -> payload.write(buf), SelectModPayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
