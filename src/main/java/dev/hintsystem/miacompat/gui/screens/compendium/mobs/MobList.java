package dev.hintsystem.miacompat.gui.screens.compendium.mobs;

import dev.hintsystem.miacompat.gui.screens.compendium.CompendiumList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public class MobList extends CompendiumList<MobSlot> {
    public static final int SLOT_GAP = 4;
    public static final int SLOT_SPACING = MobSlot.SLOT_SIZE + SLOT_GAP;

    private final Iterable<MobSlot> spawnableMobs;

    public MobList(
        Minecraft minecraft, Font font, Iterable<MobSlot> spawnableMobs,
        ScreenRectangle rectangle
    ) {
        super(minecraft, font, rectangle,
            MobSlot.SLOT_SIZE, SLOT_GAP, Component.literal("Mob List"));

        this.spawnableMobs = spawnableMobs;
    }

    public static int containerWidth(int columnCount) {
        return SLOT_SPACING * columnCount + AbstractScrollArea.SCROLLBAR_WIDTH;
    }

    public static int containerHeight(int columnCount) {
        return SLOT_SPACING * columnCount;
    }

    @Override
    protected void renderElements(GuiGraphics guiGraphics, int scrollY, int mouseX, int mouseY) {
        MobSlot hoveredMob = null;

        int itemX = this.getX();
        int itemY = scrollY;
        for (MobSlot mob : spawnableMobs) {
            if (itemY < this.getBottom()) {
                boolean hovered = isItemHovered(itemX, itemY, mouseX, mouseY);
                if (hovered && hoveredMob == null) {
                    hoveredMob = mob;
                }

                mob.render(guiGraphics, itemX, itemY);
            }

            itemX += SLOT_SPACING;
            if (itemX + MobSlot.SLOT_SIZE > this.getRight()) {
                itemX = this.getX();
                itemY += SLOT_SPACING;
            }
        }

        this.contentHeight = itemY - scrollY;
        this.hoveredSlot = hoveredMob;
    }
}
