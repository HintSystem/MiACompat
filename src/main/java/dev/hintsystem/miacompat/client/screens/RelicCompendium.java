package dev.hintsystem.miacompat.client.screens;

import dev.hintsystem.miacompat.server.ServerItemRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelicCompendium extends Screen {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("social_interactions/background");
    private static final int BG_MARGIN = 10;

    public final Map<ServerItemRegistry.RelicGrade, List<Relic>> relicsByGrade = new HashMap<>();

    public RelicCompendium() {
        super(Component.literal("Relic Compendium"));
    }

    public int windowHeight() { return Math.max(52, this.height - 128); }
    public int windowWidth() { return listWidth() + BG_MARGIN*2; }
    public int windowX() { return (this.width - windowWidth()) / 2; }
    public int windowY() { return 64; }

    public int listX() { return windowX() + BG_MARGIN; }
    public int listWidth() { return RelicList.getRowWidth(12); }

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
            listX(), windowY() + BG_MARGIN,
            RelicList.getRowWidth(12), windowHeight() - BG_MARGIN*2
        ));
    }

    public static class Relic {
        public final ServerItemRegistry.RelicConfig config;
        public final ItemStack item;

        public Relic(ServerItemRegistry.RelicConfig config) {
            this.config = config;
            this.item = new ItemStack(config.type);
            item.set(DataComponents.CUSTOM_NAME, config.name);
            item.set(DataComponents.ITEM_MODEL, config.modelId);
            item.set(DataComponents.LORE, new ItemLore(config.lore));
        }
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
