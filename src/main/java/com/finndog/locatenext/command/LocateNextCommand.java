package com.finndog.locatenext.command;

import com.finndog.locatenext.server.Msg;
import com.finndog.locatenext.server.NavigationManager;
import com.finndog.locatenext.server.NavigationState;
import com.finndog.locatenext.server.StructureCatalog;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public final class LocateNextCommand {

    /** Operators only — every branch teleports or stalls the server. */
    private static final int PERMISSION_LEVEL = 2;

    private static final SuggestionProvider<CommandSourceStack> MOD_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    StructureCatalog.byNamespace(context.getSource().getServer()).keySet(), builder);

    private LocateNextCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(build());
        // `/ln` as a redirect rather than a second tree, so the two can never drift apart.
        dispatcher.register(Commands.literal("ln")
                .requires(operatorOnly())
                .redirect(root));
    }

    /**
     * 1.21.11 replaced the numeric permission levels with a PermissionSet; LEVEL_GAMEMASTERS is
     * the named equivalent of the old level 2.
     */
    //? if >=1.21.11 {
    /*private static java.util.function.Predicate<CommandSourceStack> operatorOnly() {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }
    *///?} else {
    private static java.util.function.Predicate<CommandSourceStack> operatorOnly() {
        return source -> source.hasPermission(PERMISSION_LEVEL);
    }
    //?}

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("locatenext")
                .requires(operatorOnly())
                .executes(run(NavigationManager::status))

                .then(Commands.literal("mod")
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .suggests(MOD_IDS)
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    NavigationManager.selectMod(player, StringArgumentType.getString(context, "modid"));
                                    return 1;
                                })))

                .then(Commands.literal("mods").executes(run(NavigationManager::listMods)))
                .then(Commands.literal("list").executes(run(NavigationManager::listStructures)))
                .then(Commands.literal("status").executes(run(NavigationManager::status)))

                .then(Commands.literal("next").executes(run(player -> NavigationManager.step(player, 1))))
                .then(Commands.literal("prev").executes(run(player -> NavigationManager.step(player, -1))))
                .then(Commands.literal("variant")
                        .then(Commands.literal("next").executes(run(NavigationManager::variantNext)))
                        .then(Commands.literal("prev").executes(run(NavigationManager::variantPrev)))
                        .then(Commands.literal("list").executes(run(NavigationManager::listVariants))))
                .then(Commands.literal("home").executes(run(NavigationManager::home)))
                .then(Commands.literal("sweep").executes(run(NavigationManager::sweep)))

                .then(Commands.literal("goto")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    // Chat and /list are 1-based; the cursor is 0-based.
                                    NavigationManager.goTo(player, IntegerArgumentType.getInteger(context, "index") - 1);
                                    return 1;
                                })))

                .then(Commands.literal("radius")
                        .then(Commands.argument("chunks", IntegerArgumentType.integer(1, 2000))
                                .executes(setting((player, context) -> {
                                    int chunks = IntegerArgumentType.getInteger(context, "chunks");
                                    NavigationManager.state(player).setRadius(chunks);
                                    Msg.info(player, Msg.dim("Search radius set to " + chunks + " chunks."));
                                }))))

                .then(Commands.literal("fresh")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(setting((player, context) -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    NavigationManager.state(player).setUnexploredOnly(enabled);
                                    Msg.info(player, Msg.dim("Fresh-only " + (enabled ? "on" : "off")
                                            + " — " + (enabled
                                            ? "only ever lands on instances nobody has been sent to."
                                            : "matches vanilla /locate; nearest instance wins.")));
                                }))))

                .then(Commands.literal("autodim")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(setting((player, context) -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    NavigationManager.state(player).setAutoDimension(enabled);
                                    Msg.info(player, Msg.dim("Auto-dimension " + (enabled ? "on" : "off")
                                            + " — " + (enabled
                                            ? "jumps to whichever dimension a structure belongs to."
                                            : "only searches the dimension you're in.")));
                                }))))

                .then(Commands.literal("clear").executes(run(player -> {
                    NavigationState state = NavigationManager.state(player);
                    state.clear();
                    state.clearHome();
                    NavigationManager.markDirty(player);
                    NavigationManager.syncState(player);
                    Msg.info(player, Msg.dim("Selection and saved home cleared."));
                })));
    }

    private interface PlayerAction {
        void accept(ServerPlayer player) throws CommandSyntaxException;
    }

    private static com.mojang.brigadier.Command<CommandSourceStack> run(PlayerAction action) {
        return context -> {
            action.accept(context.getSource().getPlayerOrException());
            return 1;
        };
    }

    private static com.mojang.brigadier.Command<CommandSourceStack> setting(
            BiConsumer<ServerPlayer, CommandContext<CommandSourceStack>> action) {
        return context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            action.accept(player, context);
            // Every branch built with this helper changes a persisted setting.
            NavigationManager.markDirty(player);
            return 1;
        };
    }
}
