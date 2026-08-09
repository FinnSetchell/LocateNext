package com.finndog.locatenext.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Points Mod Menu's config button at the structure picker.
 *
 * <p>The mod has no client config to speak of — its settings are per-player and live on the server
 * — so the picker is the screen worth reaching from there. Mod Menu is a {@code compileOnly}
 * dependency and Fabric only instantiates the {@code modmenu} entrypoint when Mod Menu itself asks
 * for it, so this class is never loaded when Mod Menu is absent.
 */
public final class LocateNextModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LocateNextScreen::new;
    }
}
