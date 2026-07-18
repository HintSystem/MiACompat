package dev.hintsystem.miacompat.client.screens;

import dev.hintsystem.miacompat.server.ServerItemRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class RelicList extends AbstractScrollArea {
    public static final int GRADE_HEADER_HEIGHT = 16;
    public static final int GRADE_HEADER_MARGIN = 3;
    public static final int ITEM_GAP = 4;
    public static final int ITEM_SPACING = 16 + ITEM_GAP;
    public static final int ITEM_HOVER_MARGIN = 2;

    private final Minecraft minecraft;
    private final Font font;
    private final Map<ServerItemRegistry.RelicGrade, List<RelicCompendium.Relic>> relicsByGrade;

    private int contentHeight;

    public RelicList(
        Minecraft minecraft, Font font, Map<ServerItemRegistry.RelicGrade, List<RelicCompendium.Relic>> relicsByGrade,
        int x, int y, int width, int height
    ) {
        super(x, y, width, height, Component.literal("Relic List"));
        this.minecraft = minecraft;
        this.font = font;
        this.relicsByGrade = relicsByGrade;

        this.contentHeight = height;
    }

    public static int getRowWidth(int columnCount) { return ITEM_SPACING * columnCount + AbstractScrollArea.SCROLLBAR_WIDTH; }

    @Override
    protected int contentHeight() { return contentHeight; }

    @Override
    protected double scrollRate() { return 16; }

    private boolean isItemHovered(int itemX, int itemY, int mouseX, int mouseY) {
        return mouseX >= itemX - ITEM_HOVER_MARGIN && mouseX < itemX + 16 + ITEM_HOVER_MARGIN
            && mouseY >= itemY - ITEM_HOVER_MARGIN && mouseY < itemY + 16 + ITEM_HOVER_MARGIN;
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
        int itemX = x;
        int itemY = y;
        for (ServerItemRegistry.RelicGrade grade : ServerItemRegistry.RelicGrade.values()) {
            itemY += GRADE_HEADER_HEIGHT;
            guiGraphics.drawString(this.font, grade.displayName,
                itemX, itemY - this.font.lineHeight - GRADE_HEADER_MARGIN, -1);

            for (RelicCompendium.Relic relic : relicsByGrade.getOrDefault(grade, List.of())) {
                ItemStack item = relic.item;
                guiGraphics.renderFakeItem(item, itemX, itemY);

                if (isItemHovered(itemX, itemY, mouseX, mouseY)) {
                    guiGraphics.setTooltipForNextFrame(
                        this.font, Screen.getTooltipFromItem(this.minecraft, item), item.getTooltipImage(), mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE)
                    );
                }

                itemX += ITEM_SPACING;
                if (itemX + 16 >= x + this.getWidth()) {
                    itemX = x;
                    itemY += ITEM_SPACING;
                }
            }

            if (itemX != 0) {
                itemX = x;
                itemY += ITEM_SPACING;
            }
        }

        this.contentHeight = itemY - y;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
