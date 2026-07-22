package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.client.MiaIcons;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.JsonOps;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class MiniMessageParser {
    private static final Gson GSON = new Gson();

    /** Only resolves layer emojies */
    public static TagResolver emojyResolver(boolean stripEmojies) {
        return TagResolver.resolver("emojy", (args, ctx) -> {
            //noinspection PatternValidation
            String name = args.popOr("Missing emojy name").value();

            if (stripEmojies || !name.startsWith("layer_")) {
                return Tag.selfClosingInserting(net.kyori.adventure.text.Component.empty());
            }

            //noinspection PatternValidation
            Identifier spriteId = layerSpriteIdFromEmojyName(name);

            //noinspection PatternValidation
            SpriteObjectContents sprite = ObjectContents.sprite(
                Key.key(spriteId.getNamespace(), spriteId.getPath())
            );

            return Tag.selfClosingInserting(
                net.kyori.adventure.text.Component.object(sprite)
                    .color(NamedTextColor.WHITE)
                    .shadowColor(ShadowColor.none())
            );
        });
    }

    private static Identifier layerSpriteIdFromEmojyName(String layerEmojy) {
        int prefixLength = "layer_".length();
        String layerNoPrefix = layerEmojy.substring(prefixLength);

        String layerName = layerEmojy.substring(0, layerNoPrefix.indexOf('_'));
        return MiaIcons.getLayerSpriteId(layerName);
    }

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .tags(TagResolver.resolver(TagResolver.standard(), emojyResolver(false)))
        .build();

    public static final MiniMessage MINI_MESSAGE_STRIP_EMOJIES = MiniMessage.builder()
        .tags(TagResolver.resolver(TagResolver.standard(), emojyResolver(true)))
        .build();

    public static Component parse(String input) throws Exception {
        return parse(MINI_MESSAGE, input);
    }

    public static Component parse(String input, boolean stripEmojies) throws Exception {
        return parse(stripEmojies ? MINI_MESSAGE_STRIP_EMOJIES : MINI_MESSAGE, input);
    }

    public static Component parse(MiniMessage parser, String input) throws Exception {
        var component = parser.deserialize(input);
        var jsonString = GsonComponentSerializer.gson().serialize(component);

        return ComponentSerialization.CODEC
            .decode(JsonOps.INSTANCE, GSON.fromJson(jsonString, JsonElement.class))
            .getOrThrow()
            .getFirst();
    }
}
