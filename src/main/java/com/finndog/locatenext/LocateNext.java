package com.finndog.locatenext;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocateNext {
    public static final String MOD_ID = "locatenext";
    public static final Logger LOGGER = LoggerFactory.getLogger("LocateNext");

    private LocateNext() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * A registry key's id. 1.21.11 renamed {@code ResourceKey#location} to {@code #identifier}
     * alongside the type itself; one helper keeps that off the seven call sites.
     */
    public static ResourceLocation keyId(net.minecraft.resources.ResourceKey<?> key) {
        //? if >=1.21.11 {
        /*return key.identifier();
        *///?} else {
        return key.location();
        //?}
    }
}
