package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;

import net.minecraft.client.render.VertexConsumerProvider;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WaypointWorldRenderer.class, remap = false)
public class WaypointWorldRendererMixin {

    // Inject at the start of preRender - runs once per frame before all waypoints are rendered
    @Inject(method = "preRender", at = @At("HEAD"))
    public void beforePreRender(MinimapElementRenderInfo renderInfo, VertexConsumerProvider.Immediate vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                CallbackInfo ci) {
        SupportXaerosMinimap.setInWorldRenderer(true);
    }

    // Inject at the start of postRender - runs once per frame after all waypoints are rendered
    @Inject(method = "postRender", at = @At("HEAD"))
    public void beforePostRender(MinimapElementRenderInfo renderInfo, VertexConsumerProvider.Immediate vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                 CallbackInfo ci) {
        SupportXaerosMinimap.setInWorldRenderer(false);
    }
}