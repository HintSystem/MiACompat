package dev.hintsystem.miacompat.mixin.xaerosminimap;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.element.render.*;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderContext;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;

import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;

@Mixin(WaypointWorldRenderer.class)
public abstract class WaypointWorldRendererMixin extends MinimapElementRenderer<Waypoint, WaypointWorldRenderContext> {
    @Unique
    private static final Identifier BONFIRE_ICON_TEXTURE = Identifier.of(MiACompat.MOD_ID, "textures/gui/bonfire_icon.png");

    @Unique
    private static final RenderLayer BONFIRE_ICON = RenderLayer.of(
        "xaero_bonfire",
        786432,
        false,
        false,
        RenderPipelines.GUI_TEXTURED,
        RenderLayer.MultiPhaseParameters.builder()
            .texture(new RenderPhase.Texture(BONFIRE_ICON_TEXTURE, false))
            .target(RenderPhase.MAIN_TARGET)
            .build(false)
    );

    @Unique
    private static final VertexConsumerProvider.Immediate BONFIRE_CONSUMER = VertexConsumerProvider.immediate(new BufferAllocator(256));

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

    @Inject(
        method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)Z",
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
        VertexConsumerProvider.Immediate vanillaBufferSource,
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
        Waypoint w,
        boolean highlight,
        MatrixStack matrixStack,
        TextRenderer fontRenderer,
        VertexConsumerProvider.Immediate bufferSource,
        CallbackInfo ci
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

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        waypointBackgroundConsumer.vertex(matrix, -halfSize, -size, 0.0F).color(r, g, b, a);
        waypointBackgroundConsumer.vertex(matrix, -halfSize, 0.0F, 0.0F).color(r, g, b, a);
        waypointBackgroundConsumer.vertex(matrix, halfSize, 0.0F, 0.0F).color(r, g, b, a);
        waypointBackgroundConsumer.vertex(matrix, halfSize, -size, 0.0F).color(r, g, b, a);

        VertexConsumer vertexConsumer = BONFIRE_CONSUMER.getBuffer(BONFIRE_ICON);

        vertexConsumer.vertex(matrix, -halfSize, -size, 0.0F).color(240, 240, 240, 255).texture(0f, 0f);
        vertexConsumer.vertex(matrix, -halfSize,  0, 0.0F).color(240, 240, 240, 255).texture(0f, 1f);
        vertexConsumer.vertex(matrix,  halfSize,  0, 0.0F).color(240, 240, 240, 255).texture(1f, 1f);
        vertexConsumer.vertex(matrix,  halfSize, -size, 0.0F).color(240, 240, 240, 255).texture(1f, 0f);

        BONFIRE_CONSUMER.draw();

        ci.cancel();
    }
}