package com.finndog.locatenext.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Picks a Y to drop the player at above a located structure.
 *
 * <p>The heightmap alone is wrong in dimensions with a ceiling — in the Nether it returns the
 * bedrock roof, so teleporting there buries you in it. Those get a downward scan for the first
 * two-block air pocket over solid ground instead.
 */
public final class SafeSpot {

    private SafeSpot() {
    }

    public static BlockPos find(ServerLevel level, BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();

        if (level.dimensionType().hasCeiling()) {
            int scanned = scanUnderCeiling(level, x, z);
            if (scanned != Integer.MIN_VALUE) {
                return new BlockPos(x, scanned, z);
            }
        }

        // getHeight loads the chunk to full, so this is a real answer rather than a guess.
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(surface, level.getMinBuildHeight() + 1), z);
    }

    /** Returns {@link Integer#MIN_VALUE} when the column is solid all the way down. */
    private static int scanUnderCeiling(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int top = level.getLogicalHeight() - 1;
        int bottom = level.getMinBuildHeight() + 1;

        for (int y = top; y > bottom; y--) {
            cursor.set(x, y, z);
            BlockState feet = level.getBlockState(cursor);
            BlockState head = level.getBlockState(cursor.above());
            BlockState ground = level.getBlockState(cursor.below());
            if (feet.isAir() && head.isAir() && !ground.isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
