package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.client.MiaIcons;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;

public class MiniMessageParser {
    /** Only resolves layer emojies */
    public static TagResolver emojyResolver(boolean stripEmojies) {
        return TagResolver.resolver("emojy", (args, ctx) -> {
            String name = args.popOr("Missing emojy name").value();

            if (stripEmojies || !name.startsWith("layer_")) {
                return Tag.selfClosingInserting(net.kyori.adventure.text.Component.empty());
            }

            int prefixLength = "layer_".length();
            String layerNoPrefix = name.substring(prefixLength);

            String layerName = name.substring(0, layerNoPrefix.indexOf('_'));
            Identifier spriteId = MiaIcons.getLayerSpriteId(layerName);

            SpriteObjectContents sprite = ObjectContents
                .sprite(spriteId);

            return Tag.selfClosingInserting(
                net.kyori.adventure.text.Component.object(sprite)
                    .color(NamedTextColor.WHITE)
                    .shadowColor(ShadowColor.none())
            );
        });
    }

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .tags(TagResolver.resolver(TagResolver.standard(), emojyResolver(false)))
        .build();

    public static final MiniMessage MINI_MESSAGE_STRIP_EMOJIES = MiniMessage.builder()
        .tags(TagResolver.resolver(TagResolver.standard(), emojyResolver(true)))
        .build();

    public static Component parse(String input) {
        return parse(MINI_MESSAGE, input);
    }

    public static Component parse(String input, boolean stripEmojies) {
        return parse(stripEmojies ? MINI_MESSAGE_STRIP_EMOJIES : MINI_MESSAGE, input);
    }

    public static Component parse(MiniMessage parser, String input) {
        net.kyori.adventure.text.Component component = parser.deserialize(input);

        return MinecraftClientAudiences.of().asNative(component);
    }
}
