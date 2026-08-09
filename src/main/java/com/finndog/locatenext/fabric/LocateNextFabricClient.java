package com.finndog.locatenext.fabric;

import com.finndog.locatenext.client.ClientStructureIndex;
import com.finndog.locatenext.client.LocateNextKeys;
import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.StructureIndexPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class LocateNextFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(StructureIndexPayload.TYPE, (payload, context) ->
                ClientStructureIndex.accept(payload.structures()));

        ClientPlayNetworking.registerGlobalReceiver(NavStatePayload.TYPE, (payload, context) ->
                ClientStructureIndex.acceptState(payload.namespace(), payload.index()));

        LocateNextKeys.register();
    }
}
