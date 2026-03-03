package dev.hintsystem.miacompat.gui;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.CooldownTracker;
import dev.hintsystem.miacompat.client.InventoryTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class GearCooldown {
    public static boolean drawGearCooldown(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if (!MiACompat.config.showItemSlotGearCooldowns) return false;

        Identifier modelId = InventoryTracker.getMiAModelId(itemStack);
        if (modelId == null) return false;

        CooldownTracker.GearCooldown gearCooldown = CooldownTracker.getGearCooldown(modelId);
        if (gearCooldown == null) return false;

        float leftPercent  = gearCooldown.leftClick != null ? gearCooldown.leftClick.getPercent() : 0f;
        float rightPercent = gearCooldown.rightClick != null ? gearCooldown.rightClick.getPercent() : 0f;

        if (leftPercent > 0f && rightPercent > 0f) {
            drawBar(guiGraphics, x, y, 8, leftPercent);
            drawBar(guiGraphics, x + 8, y, 8, rightPercent);
        } else if (leftPercent > 0f) {
            drawBar(guiGraphics, x, y, 16, leftPercent);
        } else if (rightPercent > 0f) {
            drawBar(guiGraphics, x, y, 16, rightPercent);
        }

        return true;
    }

    public static void drawBar(GuiGraphics guiGraphics, int x, int y, int width, float percent) {
        int top = (int) (y + Math.floor(16.0F * (1.0F - percent)));
        int bottom = (int) (top + Math.ceil(16.0F * percent));
        guiGraphics.fill(x, top, x + width, bottom, Integer.MAX_VALUE);
    }
}
