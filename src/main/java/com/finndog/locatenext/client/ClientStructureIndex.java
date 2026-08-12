package com.finndog.locatenext.client;

import com.finndog.locatenext.net.NavigatePayload;
import com.finndog.locatenext.net.SelectModPayload;
import com.finndog.locatenext.server.StructureCatalog;
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

    public static void navigate(int op) {
        ClientNet.send(NavigatePayload.of(op));
    }

    public static void goTo(int index) {
        ClientNet.send(new NavigatePayload(NavigatePayload.OP_GOTO, index));
    }

    public static void selectMod(String namespace) {
        selectedNamespace = namespace;
        selectedIndex = -1;
        ClientNet.send(new SelectModPayload(namespace));
    }
}
