package dev.hintsystem.miacompat.client.screens;

import dev.hintsystem.miacompat.server.ServerItemRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelicCompendium extends Screen {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("social_interactions/background");
    private static final int BG_MARGIN = 8;

    public final Map<ServerItemRegistry.RelicGrade, List<Relic>> relicsByGrade = new HashMap<>();

    public RelicCompendium() {
        super(Component.literal("Relic Compendium"));
    }

    public int windowX() { return (this.width - windowWidth()) / 2; }
    public int windowY() { return 64; }
    public int windowWidth() { return listWidth() + BG_MARGIN*2; }
    public int windowHeight() { return Math.max(52, this.height - 128); }

    public int listX() { return windowX() + BG_MARGIN; }
    public int listY() { return windowY() + BG_MARGIN; }
    public int listWidth() { return RelicList.containerWidth(10); }
    public int listHeight() { return windowHeight() - BG_MARGIN*2; }

    @Override
    protected void init() {
        relicsByGrade.clear();

        for (ServerItemRegistry.ItemConfig itemConfig : ServerItemRegistry.getAllItems().values()) {
            if (!(itemConfig instanceof ServerItemRegistry.RelicConfig relicConfig)) continue;

            relicsByGrade
                .computeIfAbsent(relicConfig.grade, (k) -> new ArrayList<>())
                .add(new Relic(relicConfig));
        }

        this.addRenderableWidget(new RelicList(
            this.minecraft, this.font, this.relicsByGrade,
            listX(), listY(), listWidth(), listHeight()
        ));
    }

    public static class Relic {
        public final ServerItemRegistry.RelicConfig config;
        public final ItemStack item;

        public final int borderColor;

        public Relic(ServerItemRegistry.RelicConfig config) {
            this.config = config;
            this.item = new ItemStack(config.type);

            TextColor textColor = config.grade.displayName.getStyle().getColor();
            if (config.lore.size() >= 2) {
                textColor = config.lore.get(1).getStyle().getColor();
            }

            borderColor = textColor != null ? textColor.getValue() : 0;

            List<Component> lore = new ArrayList<>(config.lore);
            lore.set(0, Component.literal(
                config.grade.displayName.getString() + " Relic"
            ));

            item.set(DataComponents.CUSTOM_NAME, config.name);
            item.set(DataComponents.ITEM_MODEL, config.modelId);
            item.set(DataComponents.LORE, new ItemLore(lore));
        }

        public List<Component> getTooltip(Minecraft minecraft) {
            if (isHidden()) {
                return List.of(
                    Component.literal("???").setStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY).withItalic(true))
                );
            }
            return Screen.getTooltipFromItem(minecraft, item);
        }

        public boolean isUnlocked() { return true; }

        public boolean isHidden() { return false; }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, windowX(), windowY(), windowWidth(), windowHeight());
    }
}
