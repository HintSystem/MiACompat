package dev.hintsystem.miacompat.gui.screens.compendium.relics;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.CompendiumTracker;
import dev.hintsystem.miacompat.gui.MiaIcons;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.server.config.mythic.drop.ItemDrop;
import dev.hintsystem.miacompat.server.config.mythic.drop.MobDrop;
import dev.hintsystem.miacompat.server.config.mythic.drop.RelicLayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public class RelicSlot {
    private static final Identifier SLOT_SPRITE = MiACompat.id("compendium/slot_background");
    private static final Identifier SLOT_BORDER_SPRITE = MiACompat.id("compendium/slot_border");

    private static final int SLOT_BG_COLOR = 0xFF6F5234;
    private static final int SLOT_BG_COLOR_HIDDEN = 0xFF211A23;
    private static final float SLOT_HOVER_ALPHA = 0.2f;

    public static final int SLOT_SIZE = 24;
    public static final int SLOT_ITEM_PADDING = (SLOT_SIZE - 16) / 2;

    public final RelicConfig config;
    public final List<MobDrop<ItemDrop>> drops;
    public final ItemStack item;

    public final int borderColor;

    public RelicSlot(RelicConfig config, @Nullable List<MobDrop<ItemDrop>> drops) {
        this.config = config;
        this.drops = drops != null ? drops : List.of();
        this.item = new ItemStack(config.type);

        TextColor textColor = config.grade.displayName.getStyle().getColor();
        if (config.lore.size() >= 2) {
            textColor = config.lore.get(1).getStyle().getColor();
        }

        this.borderColor = textColor != null ? textColor.getValue() : 0;

        List<Component> lore = new ArrayList<>(config.lore);
        lore.set(0, Component.literal(
            config.grade.displayName.getString() + " Relic"
        ));

        item.set(DataComponents.CUSTOM_NAME, config.name);
        item.set(DataComponents.ITEM_MODEL, config.modelId);
        item.set(DataComponents.LORE, new ItemLore(lore));
    }

    public boolean isDiscovered() {
        return CompendiumTracker.isRelicDiscovered(config.prefabId);
    }

    public boolean isHidden() {
        return (!isDiscovered() && !MiACompat.config.showUndiscoveredRelics)
            && !drops.isEmpty();
    }

    public int layerOrder() {
        if (drops.isEmpty()) return Integer.MIN_VALUE;

        return RelicLayer.fromMobDrop(drops.getFirst())
            .map(Enum::ordinal)
            .orElse(Integer.MIN_VALUE);
    }

    public List<Component> getTooltip(Minecraft minecraft) {
        MutableComponent dropChances = Component.empty();
        for (var mobDrop : drops) {
            RelicLayer.fromMobDrop(mobDrop).ifPresent((l) -> {
                dropChances.append(
                    MiaIcons.getLayerSpriteComponent(l.info.iconName)
                ).append(" ");
            });

            dropChances.append(
                Component.literal(
                    String.format(Locale.ROOT, "%.3f", mobDrop.drop().chance * 100)
                        .replaceAll("\\.?0+$", "")
                ).append("% ").withStyle(ChatFormatting.GRAY));
        }

        if (isHidden()) {
            return List.of(
                Component.literal("???").setStyle(Style.EMPTY
                    .withColor(ChatFormatting.GRAY).withItalic(true)),
                dropChances
            );
        }

        List<Component> tooltip = Screen.getTooltipFromItem(minecraft, item);
        if (!dropChances.equals(Component.empty()))
            tooltip.add(1, dropChances);

        return tooltip;
    }


    public void render(GuiGraphics guiGraphics, Font font, int x, int y, boolean hovered) {
        int borderColor = hovered ? ARGB.color(1f, this.borderColor) : ARGB.color(0.7f, this.borderColor);
        if (isHidden())
            borderColor = ARGB.white(0.1f);

        // border underlay
        if (!hovered && (isDiscovered() || isHidden())) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                SLOT_BORDER_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, borderColor);
        }

        // background
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
            SLOT_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, isHidden() ? SLOT_BG_COLOR_HIDDEN : SLOT_BG_COLOR);

        if (isHidden()) {
            int xPadding = (SLOT_SIZE - font.width("?")) / 2 ;
            int yPadding = (SLOT_SIZE - font.lineHeight) / 2 ;
            guiGraphics.drawString(font, "?", x + xPadding, y + yPadding, ARGB.white(1f));
        } else {
            guiGraphics.renderFakeItem(item, x + SLOT_ITEM_PADDING, y + SLOT_ITEM_PADDING);
        }

        if (hovered) {
            // slot highlight
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                SLOT_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, SLOT_HOVER_ALPHA);

            // border overlay
            if (isDiscovered() || isHidden())
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    SLOT_BORDER_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, borderColor);
        }
    }
}
