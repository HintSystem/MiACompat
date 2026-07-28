package dev.hintsystem.miacompat.gui.screens.compendium.relics;

import dev.hintsystem.miacompat.server.config.geary.item.RelicGrade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RelicList extends AbstractScrollArea {
    public static final int SLOT_GAP = 2;
    public static final int SLOT_SPACING = RelicSlot.SLOT_SIZE + SLOT_GAP;

    public static final int GRADE_HEADER_HEIGHT = SLOT_SPACING;
    public static final int GRADE_HEADER_PADDING = 3;

    private final Minecraft minecraft;
    private final Font font;
    private final Map<RelicGrade, List<RelicSlot>> relicsByGrade;

    private int contentHeight;
    public RelicSlot hoveredRelic;

    public RelicList(
        Minecraft minecraft, Font font, Map<RelicGrade, List<RelicSlot>> relicsByGrade,
        ScreenRectangle rectangle
    ) {
        super(rectangle.left(), rectangle.top(), rectangle.width(), rectangle.height(),
            Component.literal("Relic List"));

        this.minecraft = minecraft;
        this.font = font;
        this.relicsByGrade = relicsByGrade;

        this.contentHeight = height;
    }

    public void setRectangle(ScreenRectangle rectangle) {
        setRectangle(rectangle.width(), rectangle.height(), rectangle.left(), rectangle.top());
    }

    public static int containerWidth(int columnCount) {
        return SLOT_SPACING * columnCount + AbstractScrollArea.SCROLLBAR_WIDTH;
    }

    @Override
    protected int contentHeight() { return contentHeight; }

    @Override
    protected double scrollRate() { return SLOT_SPACING; }

    private boolean isItemHovered(int itemX, int itemY, int mouseX, int mouseY) {
        int rightGap = Math.floorDiv(SLOT_GAP, 2);
        int leftGap = SLOT_GAP - rightGap;

        return mouseX >= itemX - leftGap && mouseX < itemX + RelicSlot.SLOT_SIZE + rightGap
            && mouseY >= itemY - leftGap && mouseY < itemY + RelicSlot.SLOT_SIZE + rightGap;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        if (hoveredRelic == null)
            updateScrolling(event);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        renderElements(guiGraphics,
            this.getX(), (int) (this.getY() - this.scrollAmount()),
            mouseX, mouseY
        );
        guiGraphics.disableScissor();

        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    private void renderElements(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        RelicSlot hoveredRelic = null;

        int itemX = x;
        int itemY = y;
        for (RelicGrade grade : RelicGrade.values()) {
            itemY += GRADE_HEADER_HEIGHT;
            guiGraphics.drawString(this.font, grade.displayName,
                itemX + RelicSlot.SLOT_ITEM_PADDING, itemY - this.font.lineHeight - GRADE_HEADER_PADDING, -1);

            Iterator<RelicSlot> it = relicsByGrade.getOrDefault(grade, List.of()).iterator();
            while (it.hasNext()) {
                RelicSlot relicSlot = it.next();

                boolean hovered = isItemHovered(itemX, itemY, mouseX, mouseY);
                if (hovered && hoveredRelic == null) {
                    hoveredRelic = relicSlot;
                    renderSlotTooltip(guiGraphics, relicSlot, mouseX, mouseY);
                }

                relicSlot.render(guiGraphics, this.font, itemX, itemY, hovered);

                if (!it.hasNext()) continue;

                itemX += SLOT_SPACING;
                if (itemX + SLOT_SPACING >= x + this.getWidth()) {
                    itemX = x;
                    itemY += SLOT_SPACING;
                }
            }

            itemX = x;
            itemY += SLOT_SPACING;
        }

        this.contentHeight = itemY - y;
        this.hoveredRelic = hoveredRelic;
    }

    public void renderSlotTooltip(GuiGraphics guiGraphics, RelicSlot relic, int mouseX, int mouseY) {
        ItemStack item = relic.item;
        boolean isHidden = relic.isHidden();

        guiGraphics.setTooltipForNextFrame(
            this.font, relic.getTooltip(this.minecraft),
            isHidden ? Optional.empty() : item.getTooltipImage(),
            mouseX, mouseY,
            isHidden ? null : item.get(DataComponents.TOOLTIP_STYLE)
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
