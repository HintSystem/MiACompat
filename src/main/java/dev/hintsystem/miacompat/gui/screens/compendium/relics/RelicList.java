package dev.hintsystem.miacompat.gui.screens.compendium.relics;

import dev.hintsystem.miacompat.gui.screens.compendium.CompendiumList;
import dev.hintsystem.miacompat.server.config.geary.item.RelicGrade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RelicList extends CompendiumList<RelicSlot> {
    public static final int SLOT_GAP = 2;
    public static final int SLOT_SPACING = RelicSlot.SLOT_SIZE + SLOT_GAP;

    public static final int GRADE_HEADER_HEIGHT = SLOT_SPACING;
    public static final int GRADE_HEADER_PADDING = 3;

    private final Map<RelicGrade, List<RelicSlot>> relicsByGrade;

    public RelicList(
        Minecraft minecraft, Font font, Map<RelicGrade, List<RelicSlot>> relicsByGrade,
        ScreenRectangle rectangle
    ) {
        super(minecraft, font, rectangle,
            RelicSlot.SLOT_SIZE, SLOT_GAP, Component.literal("Relic List"));

        this.relicsByGrade = relicsByGrade;
    }

    public static int containerWidth(int columnCount) {
        return SLOT_SPACING * columnCount + AbstractScrollArea.SCROLLBAR_WIDTH;
    }

    @Override
    public void playDownSound(SoundManager handler) {}

    @Override
    protected void renderElements(GuiGraphics guiGraphics, int scrolledY, int mouseX, int mouseY) {
        RelicSlot hoveredRelic = null;

        int itemX = this.getX();
        int itemY = scrolledY;
        for (RelicGrade grade : RelicGrade.values()) {
            itemY += GRADE_HEADER_HEIGHT;
            guiGraphics.drawString(this.font, grade.displayName,
                itemX + RelicSlot.SLOT_ITEM_PADDING, itemY - this.font.lineHeight - GRADE_HEADER_PADDING, -1);

            Iterator<RelicSlot> it = relicsByGrade.getOrDefault(grade, List.of()).iterator();
            while (it.hasNext()) {
                RelicSlot relicSlot = it.next();

                if (itemY < this.getBottom()) {
                    boolean hovered = isItemHovered(itemX, itemY, mouseX, mouseY);
                    if (hovered && hoveredRelic == null) {
                        hoveredRelic = relicSlot;
                        renderSlotTooltip(guiGraphics, relicSlot, mouseX, mouseY);
                    }

                    relicSlot.render(guiGraphics, this.font, itemX, itemY, hovered);
                }

                if (!it.hasNext()) continue;

                itemX += SLOT_SPACING;
                if (itemX + SLOT_SPACING > this.getRight()) {
                    itemX = this.getX();
                    itemY += SLOT_SPACING;
                }
            }

            itemX = this.getX();
            itemY += SLOT_SPACING;
        }

        this.contentHeight = itemY - scrolledY;
        this.hoveredSlot = hoveredRelic;
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
}
