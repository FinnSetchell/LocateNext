package com.finndog.locatenext.client;

import com.finndog.locatenext.net.NavigatePayload;
import com.mojang.blaze3d.platform.InputConstants;
// Unconditional: 1.21.11's keybind Category is built from a mod id, below the 26.1 module rename.
import com.finndog.locatenext.LocateNext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
// 26.3 dropped GLFW entirely (Minecraft's windowing moved to SDL) and InputConstants gained its
// own KEY_* constants, so GLFW's raw key codes are only needed below that.
//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?}

//? if fabric {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//?}
// 26.1 renamed Fabric's module from key-binding to key-mapping, matching vanilla's own name.
//? if fabric && >=26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?}
//? if fabric && <26.1 {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}
//? if neoforge {
/*import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
*///?}
// Forge's own RegisterKeyMappingsEvent is a distinct type from NeoForge's, but registers the same
// way (event.register(KeyMapping)), so the create()/registerAll() body just below is shared.
//? if forge {
/*import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
*///?}

/**
 * Arrow keys drive the cursor. They are unbound in vanilla, so this steals nothing — and holding
 * one repeats naturally through {@code consumeClick}.
 *
 * <p>Fabric creates, registers and hooks the tick loop for these all in {@link #register}. NeoForge
 * can only register a {@link KeyMapping} from inside {@code RegisterKeyMappingsEvent}, so there the
 * client entrypoint calls {@link #create} eagerly and {@link #registerAll} from that event, then
 * hooks {@link #tick} itself from {@code ClientTickEvent.Post}.
 */
public final class LocateNextKeys {

    // 1.21.11 made the category a registered value keyed by an Identifier rather than a bare
    // translation key, so the lang file carries both spellings.
    //? if >=1.21.11 {
    /*private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(LocateNext.id("main"));
    *///?} else {
    private static final String CATEGORY = "key.categories.locatenext";
    //?}

    private static KeyMapping next;
    private static KeyMapping prev;
    private static KeyMapping variantNext;
    private static KeyMapping variantPrev;
    private static KeyMapping menu;

    private LocateNextKeys() {
    }

    //? if fabric {
    // 26.3 dropped GLFW's raw key codes for InputConstants' own KEY_* constants; see the import
    // note above.
    //? if >=26.3 {
    /*private static final int KEY_RIGHT = InputConstants.KEY_RIGHT;
    private static final int KEY_LEFT = InputConstants.KEY_LEFT;
    private static final int KEY_UP = InputConstants.KEY_UP;
    private static final int KEY_DOWN = InputConstants.KEY_DOWN;
    private static final int KEY_BACKSLASH = InputConstants.KEY_BACKSLASH;
    *///?} else {
    private static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;
    private static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
    private static final int KEY_UP = GLFW.GLFW_KEY_UP;
    private static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
    private static final int KEY_BACKSLASH = GLFW.GLFW_KEY_BACKSLASH;
    //?}

    public static void register() {
        next = bind("key.locatenext.next", KEY_RIGHT);
        prev = bind("key.locatenext.prev", KEY_LEFT);
        variantNext = bind("key.locatenext.variant_next", KEY_UP);
        variantPrev = bind("key.locatenext.variant_prev", KEY_DOWN);
        menu = bind("key.locatenext.screen", KEY_BACKSLASH);

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    // Three independent, mutually exclusive conditions rather than an if/elif/else chain — see
    // the same note on LocateNextSavedData#get.
    private static KeyMapping bind(String translationKey, int key) {
        //? if >=26.3 {
        /*return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.Type.KEYBOARD, key, CATEGORY));
        *///?}
        //? if >=26.1 && <26.3 {
        /*return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, key, CATEGORY));
        *///?}
        //? if <26.1 {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, key, CATEGORY));
        //?}
    }
    //?}

    // Split into two sibling blocks rather than nesting a >=26.3 marker inside this one — Stonecutter
    // does not resolve //? markers nested inside another conditional's commented-out region (see the
    // neoforge/ entrypoint files for the same rule applied to whole classes).
    //? if neoforge && <26.3 {
    /*public static void create() {
        next = new KeyMapping("key.locatenext.next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, CATEGORY);
        prev = new KeyMapping("key.locatenext.prev", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, CATEGORY);
        variantNext = new KeyMapping("key.locatenext.variant_next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, CATEGORY);
        variantPrev = new KeyMapping("key.locatenext.variant_prev", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, CATEGORY);
        menu = new KeyMapping("key.locatenext.screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, CATEGORY);
    }

    public static void registerAll(RegisterKeyMappingsEvent event) {
        event.register(next);
        event.register(prev);
        event.register(variantNext);
        event.register(variantPrev);
        event.register(menu);
    }
    *///?}

    // 26.3 dropped GLFW entirely and renamed InputConstants.Type.KEYSYM to KEYBOARD — see the
    // import note above.
    //? if neoforge && >=26.3 {
    /*public static void create() {
        next = new KeyMapping("key.locatenext.next", InputConstants.Type.KEYBOARD, InputConstants.KEY_RIGHT, CATEGORY);
        prev = new KeyMapping("key.locatenext.prev", InputConstants.Type.KEYBOARD, InputConstants.KEY_LEFT, CATEGORY);
        variantNext = new KeyMapping("key.locatenext.variant_next", InputConstants.Type.KEYBOARD, InputConstants.KEY_UP, CATEGORY);
        variantPrev = new KeyMapping("key.locatenext.variant_prev", InputConstants.Type.KEYBOARD, InputConstants.KEY_DOWN, CATEGORY);
        menu = new KeyMapping("key.locatenext.screen", InputConstants.Type.KEYBOARD, InputConstants.KEY_BACKSLASH, CATEGORY);
    }

    public static void registerAll(RegisterKeyMappingsEvent event) {
        event.register(next);
        event.register(prev);
        event.register(variantNext);
        event.register(variantPrev);
        event.register(menu);
    }
    *///?}

    // Identical body to the NeoForge block above — Forge's own RegisterKeyMappingsEvent registers
    // the same way, just imported from a different package (see the imports above). Kept as its
    // own block rather than widening the neoforge condition to avoid relying on unproven `||`
    // support in Stonecutter's condition grammar.
    //? if forge {
    /*public static void create() {
        next = new KeyMapping("key.locatenext.next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, CATEGORY);
        prev = new KeyMapping("key.locatenext.prev", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, CATEGORY);
        variantNext = new KeyMapping("key.locatenext.variant_next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, CATEGORY);
        variantPrev = new KeyMapping("key.locatenext.variant_prev", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, CATEGORY);
        menu = new KeyMapping("key.locatenext.screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, CATEGORY);
    }

    public static void registerAll(RegisterKeyMappingsEvent event) {
        event.register(next);
        event.register(prev);
        event.register(variantNext);
        event.register(variantPrev);
        event.register(menu);
    }
    *///?}

    public static void tick() {
        // A screen being open means the arrows belong to that screen, not to us. 26.1 dropped
        // Minecraft's public screen field; the guard is redundant there anyway, because vanilla
        // only feeds key presses to KeyMapping while no screen has input.
        //? if >=26.1 {
        /*if (Minecraft.getInstance().player == null) {
            return;
        }
        *///?} else {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().screen != null) {
            return;
        }
        //?}
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
            //? if >=26.1 {
            /*Minecraft.getInstance().setScreenAndShow(new LocateNextScreen());
            *///?} else {
            Minecraft.getInstance().setScreen(new LocateNextScreen());
            //?}
        }
    }
}
