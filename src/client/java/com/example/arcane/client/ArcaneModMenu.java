package com.example.arcane.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Mod Menu entrypoint; Mod Menu itself is never a required runtime dependency. */
public final class ArcaneModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ArcaneSettingsScreen::new;
	}
}
