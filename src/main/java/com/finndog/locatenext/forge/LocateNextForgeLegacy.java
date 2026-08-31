package com.finndog.locatenext.forge;

// Forge 1.20.1 common entrypoint. Oldest and most different of the eight Forge nodes: predates
// Forge's own move from net.minecraftforge.network.simple.SimpleChannel to
// net.minecraftforge.network.SimpleChannel (package relocated, and the messageBuilder API
// replaced the plain registerMessage convenience method) at 1.20.4, and predates vanilla's
// CustomPacketPayload entirely (introduced 1.20.4). See LocateNextForge for the >=1.20.4 sibling
// this duplicates the command/sync wiring of — kept as a whole separate file rather than an inner
// version split, since Stonecutter does not resolve //? markers nested inside another //? block.
//? if forge && <1.20.4 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.ClientStructureIndex;
import com.finndog.locatenext.command.LocateNextCommand;
import com.finndog.locatenext.net.NavStatePayload;
import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.net.StructureIndexPayload;
import com.finndog.locatenext.server.NavigationManager;
import com.finndog.locatenext.server.Players;
import com.finndog.locatenext.server.StructureCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// Forge entrypoint (common — loaded on the dedicated server too). Below 1.20.4, Forge's own
// networking predates any CustomPacketPayload-shaped registrar, so the channel and its messages
// are created directly here rather than through an event, and each handler hops to the main
// thread itself via NetworkEvent.Context#enqueueWork — the same shape NeoForge's own pre-1.20.5
// fork inherited from Forge (see Net#send and LocateNextForgeLegacy#CHANNEL).
@Mod(LocateNext.MOD_ID)
public final class LocateNextForgeLegacy {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            LocateNext.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public LocateNextForgeLegacy() {
        registerPayloads();

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);

        LocateNext.LOGGER.info("LocateNext ready — /locatenext mod <modid>, then arrow keys.");
    }

    private static void registerPayloads() {
        int id = 0;
        CHANNEL.registerMessage(id++, NavigatePayload.class, NavigatePayload::write, NavigatePayload::read,
                (payload, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> {
                        ServerPlayer player = ctx.getSender();
                        if (player == null) {
                            return;
                        }
                        switch (payload.op()) {
                            case NavigatePayload.OP_NEXT -> NavigationManager.step(player, 1);
                            case NavigatePayload.OP_PREV -> NavigationManager.step(player, -1);
                            case NavigatePayload.OP_GOTO -> NavigationManager.goTo(player, payload.index());
                            case NavigatePayload.OP_HOME -> NavigationManager.home(player);
                            case NavigatePayload.OP_VARIANT_NEXT -> NavigationManager.variantNext(player);
                            case NavigatePayload.OP_VARIANT_PREV -> NavigationManager.variantPrev(player);
                            default -> LocateNext.LOGGER.warn("Unknown navigate op {} from {}",
                                    payload.op(), player.getName().getString());
                        }
                    });
                    ctx.setPacketHandled(true);
                });

        CHANNEL.registerMessage(id++, SelectModPayload.class, SelectModPayload::write, SelectModPayload::read,
                (payload, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> {
                        ServerPlayer player = ctx.getSender();
                        if (player != null) {
                            NavigationManager.selectMod(player, payload.namespace());
                        }
                    });
                    ctx.setPacketHandled(true);
                });

        CHANNEL.registerMessage(id++, StructureIndexPayload.class, StructureIndexPayload::write, StructureIndexPayload::read,
                (payload, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> ClientStructureIndex.accept(payload.structures()));
                    ctx.setPacketHandled(true);
                });

        CHANNEL.registerMessage(id, NavStatePayload.class, NavStatePayload::write, NavStatePayload::read,
                (payload, ctxSupplier) -> {
                    NetworkEvent.Context ctx = ctxSupplier.get();
                    ctx.enqueueWork(() -> ClientStructureIndex.acceptState(payload.namespace(), payload.index()));
                    ctx.setPacketHandled(true);
                });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LocateNextCommand.register(event.getDispatcher());
    }

    // Fires both on a fresh player join and on a /reload, covering the same two cases Fabric
    // handles with ServerPlayConnectionEvents.JOIN and ServerLifecycleEvents.END_DATA_PACK_RELOAD.
    private void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ServerPlayer player = event.getPlayer();
            sendIndex(player);
            NavigationManager.syncState(player);
            return;
        }
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            sendIndex(player);
            refreshSelection(player);
        }
    }

    private static void sendIndex(ServerPlayer player) {
        com.finndog.locatenext.net.Net.send(player, new StructureIndexPayload(StructureCatalog.allIds(Players.server(player))));
    }

    // Rebuilds a player's list against the reloaded registry, keeping their position if possible.
    private static void refreshSelection(ServerPlayer player) {
        var state = NavigationManager.state(player);
        if (!state.hasSelection()) {
            return;
        }
        var structures = StructureCatalog.byNamespace(Players.server(player)).get(state.namespace());
        if (structures == null || structures.isEmpty()) {
            state.clear();
        } else {
            state.reselect(structures);
        }
        NavigationManager.markDirty(player);
        NavigationManager.syncState(player);
    }
}
*///?}
