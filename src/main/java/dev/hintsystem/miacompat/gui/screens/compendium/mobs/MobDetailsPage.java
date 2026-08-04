package dev.hintsystem.miacompat.gui.screens.compendium.mobs;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.config.LayerYamlSchema;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class MobDetailsPage extends AbstractWidget {
    private static final Identifier PAGE_BG_SPRITE = MiACompat.id("compendium/mob_page_background");

    public static final int WIDTH = 164;

    public static final int HEADER_MARGIN_Y = 12;

    public static final int MOB_BG_MARGIN_Y = 4;
    public static final int MOB_BG_WIDTH = 128;
    public static final int MOB_BG_HEIGHT = 64;

    public static final int MOB_SIZE = 72;

    private final Font font;
    private final Supplier<MobSlot> selectedSlotSupplier;

    public MobDetailsPage(Font font, Supplier<MobSlot> selectedSlotSupplier, ScreenRectangle rectangle) {
        super(rectangle.left(), rectangle.top(), WIDTH, rectangle.height(), Component.literal("Mob Details"));

        this.font = font;
        this.selectedSlotSupplier = selectedSlotSupplier;
    }

    @Override
    public int getWidth() { return WIDTH; }

    public void setRectangle(ScreenRectangle rectangle) {
        setRectangle(WIDTH, rectangle.height(), rectangle.left(), rectangle.top());
    }

    private ScreenRectangle getMobPreviewRect() {
        return new ScreenRectangle(
            this.getX() + (WIDTH - MOB_BG_WIDTH) / 2,
            this.getY() + HEADER_MARGIN_Y + this.font.lineHeight + MOB_BG_MARGIN_Y,
            MOB_BG_WIDTH, MOB_BG_HEIGHT
        );
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_BG_SPRITE,
            getX(), getY(),
            getWidth(), getHeight()
        );

        MobSlot selectedSlot = selectedSlotSupplier.get();
        if (selectedSlot == null) return;

        int x = this.getX() + (WIDTH - font.width(selectedSlot.name)) / 2;
        guiGraphics.drawString(font, selectedSlot.name, x, this.getY() + HEADER_MARGIN_Y, -1);

        ScreenRectangle rect = getMobPreviewRect();

        int mobX = rect.left() + (rect.width() - MOB_SIZE) / 2;
        int mobY = rect.bottom() - MOB_SIZE + 2;

        if (!selectedSlot.layers.isEmpty()) {
            LayerYamlSchema.Layer layer = selectedSlot.layers.getFirst();
            renderLayerBackground(guiGraphics, layer);
        }

        renderMobSprite(guiGraphics, selectedSlot.sprite, mobX + 2, mobY + 2, 0xC8000000);
        renderMobSprite(guiGraphics, selectedSlot.sprite, mobX, mobY, -1);
    }

    private void renderMobSprite(GuiGraphics guiGraphics, Identifier sprite, int x, int y, int color) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite,
            MOB_SIZE, MOB_SIZE, 0, 0,
            x, y,
            MOB_SIZE, MOB_SIZE,
            color
        );
    }

    private void renderLayerBackground(GuiGraphics guiGraphics, LayerYamlSchema.Layer layer) {
        if (layer == null) return;

        switch (layer.id) {
            case "layerone" -> renderPaintingSprite(guiGraphics, "skull_and_roses", 64, 64);
            case "layertwo" -> renderPaintingSprite(guiGraphics, "bust", 64, 64);
            case "layerthree" -> renderPaintingSprite(guiGraphics, "pigscene", 64, 64);
            case "layerfour" -> renderPaintingSprite(guiGraphics, "pool", 128, 64);
            case "layerfive" -> renderPaintingSprite(guiGraphics, "donkey_kong", 128, 96);
        }
    }

    private void renderPaintingSprite(GuiGraphics guiGraphics, String paintingName, int textureWidth, int textureHeight) {
        ScreenRectangle rect = getMobPreviewRect();

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, paintingId(paintingName),
            rect.left() + 1, rect.top() + 1,
            1f, 1f,
            rect.width() - 2, rect.height() - 2,
            textureWidth, textureHeight,
            0xC8FFFFFF
        );
    }

    private static Identifier paintingId(String paintingName) {
        return Identifier.withDefaultNamespace("textures/painting/" + paintingName + ".png");
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
