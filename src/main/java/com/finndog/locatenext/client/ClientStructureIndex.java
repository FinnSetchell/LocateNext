package com.finndog.locatenext.client;

import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.server.StructureCatalog;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Client-side mirror of the server's structure registry and navigation cursor.
 *
 * <p>Populated by {@code StructureIndexPayload} on join. Everything here is empty until then, so
 * the menu degrades to "no structures known" on a server without the mod rather than misbehaving.
 */
public final class ClientStructureIndex {

    private static Map<String, List<ResourceLocation>> byNamespace = Map.of();
    private static String selectedNamespace = "";
    private static int selectedIndex = -1;

    private ClientStructureIndex() {
    }

    public static void accept(List<ResourceLocation> structures) {
        byNamespace = StructureCatalog.group(structures);
    }

    public static void acceptState(String namespace, int index) {
        selectedNamespace = namespace;
        selectedIndex = index;
    }

    public static Map<String, List<ResourceLocation>> byNamespace() {
        return byNamespace;
    }

    public static List<ResourceLocation> structures(String namespace) {
        return byNamespace.getOrDefault(namespace, List.of());
    }

    public static String selectedNamespace() {
        return selectedNamespace;
    }

    public static int selectedIndex() {
        return selectedIndex;
    }

    public static boolean isEmpty() {
        return byNamespace.isEmpty();
    }

    // ------------------------------------------------------------------ outgoing

    /** No-op when the server doesn't have the mod, so keybinds stay silent instead of erroring. */
    private static void send(CustomPacketPayload payload, CustomPacketPayload.Type<?> type) {
        if (ClientPlayNetworking.canSend(type)) {
            ClientPlayNetworking.send(payload);
        }
    }

    public static void navigate(int op) {
        send(NavigatePayload.of(op), NavigatePayload.TYPE);
    }

    public static void goTo(int index) {
        send(new NavigatePayload(NavigatePayload.OP_GOTO, index), NavigatePayload.TYPE);
    }

    public static void selectMod(String namespace) {
        selectedNamespace = namespace;
        selectedIndex = -1;
        send(new SelectModPayload(namespace), SelectModPayload.TYPE);
    }
}
