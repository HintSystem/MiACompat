package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.client.tooltip.BundleTooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.component.BundleContents;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin {
    @Final @Shadow
    private BundleContents contents;

    @Inject(method = "drawSelectedItemTooltip", at = @At("HEAD"), cancellable = true)
    private void miacompat$drawItemTooltipWithLore(
        Font font,
        GuiGraphics graphics,
        int x,
        int y,
        int w,
        CallbackInfo ci
    ) {
        if (BundleTooltip.onDrawItemTooltip(contents, font, graphics, x, y, w)) ci.cancel();
    }
}
