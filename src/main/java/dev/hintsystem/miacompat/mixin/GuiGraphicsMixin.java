package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.gui.GearCooldown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Inject(method = "renderItemCooldown", at = @At("HEAD"), cancellable = true)
    public void miacompat$renderGearCooldown(ItemStack itemStack, int x, int y, CallbackInfo ci) {
        if (GearCooldown.drawGearCooldown((GuiGraphics)(Object) this, itemStack, x, y)) ci.cancel();
    }
}
