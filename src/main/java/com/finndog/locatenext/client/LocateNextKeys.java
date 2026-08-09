package com.finndog.locatenext.client;

import com.finndog.locatenext.net.NavigatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Arrow keys drive the cursor. They are unbound in vanilla, so this steals nothing — and holding
 * one repeats naturally through {@code consumeClick}.
 */
public final class LocateNextKeys {

    private static final String CATEGORY = "key.categories.locatenext";

    private static KeyMapping next;
    private static KeyMapping prev;
    private static KeyMapping variantNext;
    private static KeyMapping variantPrev;
    private static KeyMapping menu;

    private LocateNextKeys() {
    }

    public static void register() {
        // ◀ ▶ move through the mod's structure list; ↑ ↓ move through instances of the one you're on.
        next = bind("key.locatenext.next", GLFW.GLFW_KEY_RIGHT);
        prev = bind("key.locatenext.prev", GLFW.GLFW_KEY_LEFT);
        variantNext = bind("key.locatenext.variant_next", GLFW.GLFW_KEY_UP);
        variantPrev = bind("key.locatenext.variant_prev", GLFW.GLFW_KEY_DOWN);
        menu = bind("key.locatenext.screen", GLFW.GLFW_KEY_BACKSLASH);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // A screen being open means the arrows belong to that screen, not to us.
            if (client.player == null || client.screen != null) {
                return;
            }
            while (next.consumeClick()) {
                ClientStructureIndex.navigate(NavigatePayload.OP_NEXT);
            }
            while (prev.consumeClick()) {
                ClientStructureIndex.navigate(NavigatePayload.OP_PREV);
            }
            while (variantNext.consumeClick()) {
                ClientStructureIndex.navigate(NavigatePayload.OP_VARIANT_NEXT);
            }
            while (variantPrev.consumeClick()) {
                ClientStructureIndex.navigate(NavigatePayload.OP_VARIANT_PREV);
            }
            if (menu.consumeClick()) {
                client.setScreen(new LocateNextScreen());
            }
        });
    }

    private static KeyMapping bind(String translationKey, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, key, CATEGORY));
    }
}
