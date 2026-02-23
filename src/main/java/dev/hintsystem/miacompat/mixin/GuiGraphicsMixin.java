package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.CooldownTracker;
import dev.hintsystem.miacompat.InventoryTracker;

import dev.hintsystem.miacompat.MiACompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Inject(method = "renderItemCooldown", at = @At("HEAD"), cancellable = true)
    public void miacompat$renderGearCooldown(ItemStack itemStack, int i, int j, CallbackInfo ci) {
        if (!MiACompat.config.showItemSlotGearCooldowns) return;

        Identifier modelId = InventoryTracker.getMiAModelId(itemStack);
        if (modelId == null) return;

        ci.cancel();

        CooldownTracker.GearCooldown gearCooldown = CooldownTracker.getGearCooldown(modelId);
        if (gearCooldown == null) return;

        float leftPercent  = gearCooldown.leftClick != null ? gearCooldown.leftClick.getPercent() : 0f;
        float rightPercent = gearCooldown.rightClick != null ? gearCooldown.rightClick.getPercent() : 0f;

        if (leftPercent > 0f && rightPercent > 0f) {
            miacompat$renderBar(i, j, 8, leftPercent);
            miacompat$renderBar(i + 8, j, 8, rightPercent);
        } else if (leftPercent > 0f) {
            miacompat$renderBar(i, j, 16, leftPercent);
        } else if (rightPercent > 0f) {
            miacompat$renderBar(i, j, 16, rightPercent);
        }
    }

    @Unique
    private void miacompat$renderBar(int x, int y, int width, float percent) {
        int top = (int) (y + Math.floor(16.0F * (1.0F - percent)));
        int bottom = (int) (top + Math.ceil(16.0F * percent));
        ((GuiGraphics)(Object)this).fill(x, top, x + width, bottom, Integer.MAX_VALUE);
    }
}
