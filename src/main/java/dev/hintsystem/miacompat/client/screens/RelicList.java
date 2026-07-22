package dev.hintsystem.miacompat.client.screens;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.config.geary.item.RelicGrade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RelicList extends AbstractScrollArea {
    private static final Identifier SLOT_SPRITE = MiACompat.id("compendium/slot_background");
    private static final Identifier SLOT_BORDER_SPRITE = MiACompat.id("compendium/slot_border");

    public static final int SLOT_BG_COLOR = 0xFF6F5234;
    public static final int SLOT_BG_COLOR_HIDDEN = 0xFF211A23;
    public static final float SLOT_HOVER_ALPHA = 0.2f;

    public static final int SLOT_GAP = 2;
    public static final int SLOT_SIZE = 24;
    public static final int SLOT_SPACING = SLOT_SIZE + SLOT_GAP;
    public static final int SLOT_ITEM_PADDING = (SLOT_SIZE - 16) / 2;

    public static final int GRADE_HEADER_HEIGHT = SLOT_SPACING;
    public static final int GRADE_HEADER_PADDING = 3;

    private final Minecraft minecraft;
    private final Font font;
    private final Map<RelicGrade, List<RelicCompendium.Relic>> relicsByGrade;

    private int contentHeight;

    public RelicList(
        Minecraft minecraft, Font font, Map<RelicGrade, List<RelicCompendium.Relic>> relicsByGrade,
        int x, int y, int width, int height
    ) {
        super(x, y, width, height, Component.literal("Relic List"));
        this.minecraft = minecraft;
        this.font = font;
        this.relicsByGrade = relicsByGrade;

        this.contentHeight = height;
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

        return mouseX >= itemX - leftGap && mouseX < itemX + SLOT_SIZE + rightGap
            && mouseY >= itemY - leftGap && mouseY < itemY + SLOT_SIZE + rightGap;
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
        for (RelicGrade grade : RelicGrade.values()) {
            itemY += GRADE_HEADER_HEIGHT;
            guiGraphics.drawString(this.font, grade.displayName,
                itemX + SLOT_ITEM_PADDING, itemY - this.font.lineHeight - GRADE_HEADER_PADDING, -1);

            Iterator<RelicCompendium.Relic> it = relicsByGrade.getOrDefault(grade, List.of()).iterator();
            while (it.hasNext()) {
                RelicCompendium.Relic relic = it.next();

                boolean hovered = isItemHovered(itemX, itemY, mouseX, mouseY);

                renderSlot(guiGraphics, relic, itemX, itemY, hovered);
                if (hovered) renderSlotTooltip(guiGraphics, relic, mouseX, mouseY);

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
    }

    public void renderSlotTooltip(GuiGraphics guiGraphics, RelicCompendium.Relic relic, int mouseX, int mouseY) {
        ItemStack item = relic.item;
        boolean isHidden = relic.isHidden();

        guiGraphics.setTooltipForNextFrame(
            this.font, relic.getTooltip(this.minecraft),
            isHidden ? Optional.empty() : item.getTooltipImage(),
            mouseX, mouseY,
            isHidden ? null : item.get(DataComponents.TOOLTIP_STYLE)
        );
    }

    public void renderSlot(GuiGraphics guiGraphics, RelicCompendium.Relic relic, int x, int y, boolean hovered) {
        ItemStack item = relic.item;
        int borderColor = hovered ? ARGB.color(1f, relic.borderColor) : ARGB.color(0.7f, relic.borderColor);
        if (relic.isHidden())
            borderColor = ARGB.white(0.1f);

        if (!hovered && (relic.isUnlocked() || relic.isHidden())) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                SLOT_BORDER_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, borderColor);
        }

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
            SLOT_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, relic.isHidden() ? SLOT_BG_COLOR_HIDDEN : SLOT_BG_COLOR);

        if (relic.isHidden()) {
            int xPadding = (SLOT_SIZE - this.font.width("?")) / 2 ;
            int yPadding = (SLOT_SIZE - this.font.lineHeight) / 2 ;
            guiGraphics.drawString(this.font, "?", x + xPadding, y + yPadding, ARGB.white(1f));
        } else {
            guiGraphics.renderFakeItem(item, x + SLOT_ITEM_PADDING, y + SLOT_ITEM_PADDING);
        }

        if (hovered) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                SLOT_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, SLOT_HOVER_ALPHA);

            if (relic.isUnlocked() || relic.isHidden())
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    SLOT_BORDER_SPRITE, x, y, SLOT_SIZE, SLOT_SIZE, borderColor);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
