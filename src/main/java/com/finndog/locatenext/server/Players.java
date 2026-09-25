package com.finndog.locatenext.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two player accessors that moved in 26.1, behind one name each.
 *
 * <p>{@code ServerPlayer#level()} got a covariant {@link ServerLevel} return and
 * {@code serverLevel()} was dropped, and the entity's {@code server} field became private with no
 * {@code getServer()}. This was assumed to land in 1.21.11, but compiling against the real 1.21.10
 * artifact proved {@code serverLevel()} already gone there too (javac: cannot find symbol), so the
 * boundary moves down to 1.21.9, alongside the other reworks discovered at the same version. Both
 * accessors are used throughout, so the version split lives here rather than at a dozen call sites.
 */
public final class Players {

    private Players() {
    }

    public static ServerLevel level(ServerPlayer player) {
        //? if >=1.21.9 {
        /*return player.level();
        *///?} else {
        return player.serverLevel();
        //?}
    }

    public static MinecraftServer server(ServerPlayer player) {
        return level(player).getServer();
    }
}
