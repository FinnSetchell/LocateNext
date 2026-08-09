package com.finndog.locatenext.server;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** Chat formatting for everything the mod says. Kept in one place so the output stays consistent. */
public final class Msg {

    private static final Component PREFIX =
            Component.literal("[LN] ").withStyle(ChatFormatting.AQUA);

    private Msg() {
    }

    public static void info(ServerPlayer player, Component body) {
        player.sendSystemMessage(Component.empty().append(PREFIX).append(body));
    }

    public static void error(ServerPlayer player, String body) {
        info(player, Component.literal(body).withStyle(ChatFormatting.RED));
    }

    public static void plain(ServerPlayer player, Component body) {
        player.sendSystemMessage(body);
    }

    public static MutableComponent value(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    public static MutableComponent structure(String id) {
        return Component.literal(id).withStyle(ChatFormatting.YELLOW);
    }

    public static MutableComponent dim(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    /** A clickable chat button that runs {@code command}. */
    public static MutableComponent button(String label, String command, ChatFormatting colour) {
        return Component.literal(label).withStyle(style -> style
                .withColor(colour)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    /** Coordinates that copy to the clipboard when clicked. */
    public static MutableComponent coords(BlockPos pos) {
        String text = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to copy"))));
    }

    /** The footer under every jump report. Mirrors the arrow keys: ◀ ▶ change structure, ↑ ↓ change instance. */
    public static MutableComponent navBar() {
        return Component.empty()
                .append(button("[◀ prev]", "/locatenext prev", ChatFormatting.GOLD))
                .append(" ")
                .append(button("[next ▶]", "/locatenext next", ChatFormatting.GOLD))
                .append("  ")
                .append(button("[↑ new]", "/locatenext variant next", ChatFormatting.LIGHT_PURPLE))
                .append(" ")
                .append(button("[↓ back]", "/locatenext variant prev", ChatFormatting.LIGHT_PURPLE))
                .append("  ")
                .append(button("[home]", "/locatenext home", ChatFormatting.DARK_GRAY));
    }

    public static String formatDistance(double blocks) {
        if (blocks >= 10_000) {
            return String.format("%.1fk", blocks / 1000.0);
        }
        return String.format("%,d", Math.round(blocks));
    }

    /** Eight-point compass bearing from origin to target, for "which way did it send me". */
    public static String compass(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (dx == 0 && dz == 0) {
            return "here";
        }
        // Screen/world convention: +Z is south, +X is east.
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int sector = (int) Math.round(((angle % 360) + 360) % 360 / 45.0) % 8;
        return points[sector];
    }
}
