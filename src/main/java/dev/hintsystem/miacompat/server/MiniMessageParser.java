package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.gui.MiaIcons;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;

public class MiniMessageParser {
    /** Only resolves layer emojies */
    public static TagResolver createEmojyResolver(boolean stripEmojies) {
        return TagResolver.resolver("emojy", (args, ctx) -> {
            String name = args.popOr("Missing emojy name").value();

            if (stripEmojies || !name.startsWith("layer_")) {
                return Tag.selfClosingInserting(net.kyori.adventure.text.Component.empty());
            }

            int prefixLength = "layer_".length();
            int nextSplit = name.indexOf('_', prefixLength);

            Identifier spriteId = MiaIcons.getLayerSpriteId(
                nextSplit != -1
                    ? name.substring(0, nextSplit)
                    : name
            );

            return Tag.selfClosingInserting(
                net.kyori.adventure.text.Component
                    .object(ObjectContents.sprite(MiaIcons.ATLAS_ID, spriteId))
                    .color(NamedTextColor.WHITE)
                    .shadowColor(ShadowColor.none())
            );
        });
    }

    private static final TagResolver EMOJY_RESOLVER = createEmojyResolver(false);
    private static final TagResolver STRIPPED_EMOJY_RESOLVER = createEmojyResolver(true);

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .tags(TagResolver.standard())
        .build();

    public static String stripTags(String input) {
        if (input == null) return null;

        return MINI_MESSAGE.stripTags(input, STRIPPED_EMOJY_RESOLVER);
    }

    public static Component parse(String input) {
        return parse(input, false);
    }

    public static Component parse(String input, boolean stripEmojies) {
        net.kyori.adventure.text.Component component = MINI_MESSAGE.deserialize(
            input, stripEmojies ? STRIPPED_EMOJY_RESOLVER : EMOJY_RESOLVER
        );

        return MinecraftClientAudiences.of().asNative(component);
    }
}
