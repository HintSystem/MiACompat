package dev.hintsystem.miacompat.mixin.xaerosminimap;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;

import net.minecraft.client.renderer.MultiBufferSource;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderer;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerTrackerMinimapElementRenderer.class, remap = false)
public class PlayerTrackerMinimapElementRendererMixin {

    @Inject(method = "preRender", at = @At("HEAD"))
    public void beforePreRender(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                CallbackInfo ci) {
        SupportXaerosMinimap.setInWorldRenderer(true);
    }

    @Inject(method = "postRender", at = @At("HEAD"))
    public void beforePostRender(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                 CallbackInfo ci) {
        SupportXaerosMinimap.setInWorldRenderer(false);
    }
}
