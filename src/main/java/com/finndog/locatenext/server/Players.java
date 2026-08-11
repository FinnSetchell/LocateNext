package com.finndog.locatenext.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two player accessors that moved in 26.1, behind one name each.
 *
 * <p>26.1 gave {@code ServerPlayer#level()} a covariant {@link ServerLevel} return and dropped
 * {@code serverLevel()}, and made the {@code server} field private with no {@code getServer()} on
 * the entity. Both are used throughout, so the version split lives here rather than at a dozen
 * call sites.
 */
public final class Players {

    private Players() {
    }

    public static ServerLevel level(ServerPlayer player) {
        //? if >=26.1 {
        /*return player.level();
        *///?} else {
        return player.serverLevel();
        //?}
    }

    public static MinecraftServer server(ServerPlayer player) {
        return level(player).getServer();
    }
}
