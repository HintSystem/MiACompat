package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.mods.SupportXaeroMinimap;

import xaero.lib.XaeroLib;
import xaero.common.graphics.CustomRenderTypes;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CustomRenderTypes.class)
public class CustomRenderTypesMixin {
    @Inject(method = "applyFixedOrder", at = @At("HEAD"))
    private static void addBonfireLayer(CallbackInfo ci) {
        XaeroLib.INSTANCE.getClient()
            .getBufferProvider()
            .addToFixedOrder(SupportXaeroMinimap.GUI_BONFIRE);
    }
}
