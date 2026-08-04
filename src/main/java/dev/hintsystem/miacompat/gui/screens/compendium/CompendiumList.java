package dev.hintsystem.miacompat.gui.screens.compendium;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

public abstract class CompendiumList<T> extends AbstractScrollArea {
    protected final Minecraft minecraft;
    protected final Font font;

    protected final int slotSize;
    protected final int slotGap;

    protected int contentHeight;
    @Nullable protected T hoveredSlot;
    @Nullable protected T clickedSlot;

    public CompendiumList(
        Minecraft minecraft, Font font, ScreenRectangle rectangle,
        int slotSize, int slotGap, Component title
    ) {
        super(rectangle.left(), rectangle.top(), rectangle.width(), rectangle.height(), title);

        this.minecraft = minecraft;
        this.font = font;

        this.slotSize = slotSize;
        this.slotGap = slotGap;

        this.contentHeight = height;
    }

    @Nullable public T getHoveredSlot() { return hoveredSlot; }
    @Nullable public T getClickedSlot() { return clickedSlot; }

    public void setRectangle(ScreenRectangle rectangle) {
        setRectangle(rectangle.width(), rectangle.height(), rectangle.left(), rectangle.top());
    }

    @Override
    protected int contentHeight() { return contentHeight; }

    @Override
    protected double scrollRate() { return slotSize + slotGap; }

    @Override
    public void playDownSound(SoundManager handler) {
        if (hoveredSlot == null) return;

        super.playDownSound(handler);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        if (hoveredSlot == null) {
            updateScrolling(event);
            return;
        }

        clickedSlot = hoveredSlot;
    }

    protected boolean isItemHovered(int itemX, int itemY, int mouseX, int mouseY) {
        int rightGap = Math.floorDiv(slotGap, 2);
        int leftGap = slotGap - rightGap;

        return mouseX >= itemX - leftGap && mouseX < itemX + slotSize + rightGap
            && mouseY >= itemY - leftGap && mouseY < itemY + slotSize + rightGap;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        renderElements(guiGraphics,
            (int) (this.getY() - this.scrollAmount()),
            mouseX, mouseY
        );
        guiGraphics.disableScissor();

        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    protected abstract void renderElements(GuiGraphics guiGraphics, int scrollY, int mouseX, int mouseY);

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
