package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.mods.SupportXaeroMinimap;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderer;

import net.minecraft.client.renderer.MultiBufferSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerTrackerMinimapElementRenderer.class, remap = false)
public class PlayerTrackerMinimapElementRendererMixin {

    @Inject(method = "preRender", at = @At("HEAD"))
    public void miacompat$beforePreRender(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                CallbackInfo ci) {
        SupportXaeroMinimap.setInWorldRenderer(true);
    }

    @Inject(method = "postRender", at = @At("HEAD"))
    public void miacompat$beforePostRender(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                 CallbackInfo ci) {
        SupportXaeroMinimap.setInWorldRenderer(false);
    }
}
