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
}
