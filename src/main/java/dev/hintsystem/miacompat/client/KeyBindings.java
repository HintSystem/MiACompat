package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.screens.RelicCompendium;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeyBindings {
    public static final KeyMapping.Category CATEGORY_GENERAL = KeyMapping.Category.register(MiACompat.id("general"));

    public static final KeyMapping OPEN_RELIC_COMPENDIUM = new KeyMapping(
        "key.miacompat.open_relic_compendium",
        InputConstants.KEY_C, CATEGORY_GENERAL
    );

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
        "key.miacompat.open_config",
        InputConstants.UNKNOWN.getValue(), CATEGORY_GENERAL
    );

    public static void tickKeybinds(Minecraft client) {
        while (OPEN_RELIC_COMPENDIUM.consumeClick()) {
            client.setScreen(new RelicCompendium());
        }

        while (OPEN_CONFIG.consumeClick()) {
            client.setScreen(MiACompat.config.createScreen(client.screen));
        }
    }
}
