package dev.hintsystem.miacompat.mixin.xaerosminimap;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.element.render.*;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderContext;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;

@Mixin(value = WaypointWorldRenderer.class, remap = false)
public abstract class WaypointWorldRendererMixin extends MinimapElementRenderer<Waypoint, WaypointWorldRenderContext> {
    @Shadow(remap = false)
    private boolean dimensionScaleDistance;
    @Shadow(remap = false)
    private boolean temporaryWaypointsGlobal;
    @Shadow(remap = false)
    private int opacity;
    @Shadow
    private VertexConsumer waypointBackgroundConsumer;

    public WaypointWorldRendererMixin(MinimapElementReader<Waypoint, WaypointWorldRenderContext> elementReader, MinimapElementRenderProvider<Waypoint, WaypointWorldRenderContext> provider, WaypointWorldRenderContext context) {
        super(elementReader, provider, context);
    }

    // Inject at the end of preRender - runs once per frame before all waypoints are rendered
    @Inject(method = "preRender", at = @At("TAIL"))
    public void afterPreRender(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                CallbackInfo ci) {
        SupportXaerosMinimap.setInWorldRenderer(true);
    }

    // Inject at the start of postRender - runs once per frame after all waypoints are rendered
    @Inject(method = "postRender", at = @At("HEAD"))
    public void beforePostRender(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                 CallbackInfo ci) {
        SupportXaerosMinimap.setInWorldRenderer(false);
    }

    @Inject(
        method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    public void render(
        Waypoint w,
        boolean highlighted,
        boolean outOfBounds,
        double optionalDepth,
        float optionalScale,
        double partialX,
        double partialY,
        MinimapElementRenderInfo renderInfo,
        MinimapElementGraphics guiGraphics,
        MultiBufferSource.BufferSource vanillaBufferSource,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (MiACompat.config.maxWaypointRadius <= 0) return;

        double waypointPosDivider = renderInfo.backgroundCoordinateScale / this.context.dimCoordinateScale;

        double offX = w.getX(waypointPosDivider) + 0.5 - renderInfo.renderPos.x;
        double offY = (w.isYIncluded() ? w.getY() : renderInfo.renderEntityPos.y) + 1.0 - renderInfo.renderPos.y;
        double offZ = w.getZ(waypointPosDivider) + 0.5 - renderInfo.renderPos.z;

        double distance3D = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
        double scaledDistance = distance3D * (this.dimensionScaleDistance ? renderInfo.backgroundCoordinateScale : 1.0);

        if (
            !w.isDestination()
            && !w.isGlobal()
            && w.getPurpose() != WaypointPurpose.DEATH
            && (!w.isTemporary() || !this.temporaryWaypointsGlobal)
            && scaledDistance > MiACompat.config.maxWaypointRadius
        ) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "renderIcon", at = @At("HEAD"), cancellable = true)
    private void renderBonfireIcon(
        Waypoint w, boolean highlight, PoseStack matrixStack, Font fontRenderer, XaeroBufferProvider bufferSource, CallbackInfo ci
    ) {
        if (!"Bonfire".equals(w.getName())) return;

        int color = w.getWaypointColor().getHex();
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        float a = 0.52274513F * (float)this.opacity / 100.0F;
        if (highlight && this.context.onlyMainInfo) {
            a = Math.min(1.0F, a * 1.5F);
        }

        float halfSize = 8f;
        float size = halfSize*2;

        Matrix4f matrix = matrixStack.last().pose();
        waypointBackgroundConsumer.addVertex(matrix, -halfSize, -size, 0.0F).setColor(r, g, b, a);
        waypointBackgroundConsumer.addVertex(matrix, -halfSize, 0.0F, 0.0F).setColor(r, g, b, a);
        waypointBackgroundConsumer.addVertex(matrix, halfSize, 0.0F, 0.0F).setColor(r, g, b, a);
        waypointBackgroundConsumer.addVertex(matrix, halfSize, -size, 0.0F).setColor(r, g, b, a);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(SupportXaerosMinimap.GUI_BONFIRE);

        vertexConsumer.addVertex(matrix, -halfSize, -size, 0.0F).setUv(0f, 0f).setColor(240, 240, 240, 255);
        vertexConsumer.addVertex(matrix, -halfSize,  0, 0.0F).setUv(0f, 1f).setColor(240, 240, 240, 255);
        vertexConsumer.addVertex(matrix,  halfSize,  0, 0.0F).setUv(1f, 1f).setColor(240, 240, 240, 255);
        vertexConsumer.addVertex(matrix,  halfSize, -size, 0.0F).setUv(1f, 0f).setColor(240, 240, 240, 255);

        ci.cancel();
    }
}