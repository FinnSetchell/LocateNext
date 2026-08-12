package com.finndog.locatenext.net;

import com.finndog.locatenext.LocateNext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/** Server -> client mirror of the cursor, so the menu can highlight where you are. */
public record NavStatePayload(String namespace, int index)
        //? if >=1.20.5 {
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
