package dev.hintsystem.miacompat.mods;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.screens.ConfigScreen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class SupportModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ConfigScreen.create(parent, MiACompat.config);
    }
}
