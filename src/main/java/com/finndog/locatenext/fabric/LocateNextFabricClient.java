package com.finndog.locatenext.fabric;

//? if fabric {

import com.finndog.locatenext.client.ClientNet;
import com.finndog.locatenext.client.ClientStructureIndex;
import com.finndog.locatenext.client.LocateNextKeys;
import net.fabricmc.api.ClientModInitializer;

public final class LocateNextFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientNet.onStructureIndex(payload -> ClientStructureIndex.accept(payload.structures()));
        ClientNet.onNavState(payload ->
                ClientStructureIndex.acceptState(payload.namespace(), payload.index()));

        LocateNextKeys.register();
    }
}
//?}
