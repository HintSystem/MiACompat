package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.ChatFormatting;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;

public class MiaIcons {
    public static final String SPRITE_PATH = "mia_icons/";

    public static final Identifier ORTH_COIN_SPRITE = MiACompat.id(SPRITE_PATH + "orthcoin");

    /** @param layerName e.g. `layer_1`, `layer_orth` */
    public static AtlasSprite getLayerSprite(String layerName) {
        return getAtlasSprite(getLayerSpriteId(layerName));
    }

    /** @param layerName e.g. `layer_1`, `layer_orth` */
    public static Identifier getLayerSpriteId(String layerName) {
        return MiACompat.id(SPRITE_PATH + "prefixes/layer/" + layerName + "_square");
    }

    public static Component getSpriteComponent(Identifier id) {
        return Component.object(getAtlasSprite(id))
            .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
    }

    public static AtlasSprite getAtlasSprite(Identifier id) {
        return new AtlasSprite(AtlasIds.GUI, id);
    }
}
