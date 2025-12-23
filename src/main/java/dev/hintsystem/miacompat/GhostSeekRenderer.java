package dev.hintsystem.miacompat;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class GhostSeekRenderer {
    private static final String RENDER_LABEL = MiACompat.MOD_ID + " ghost seek renderer";
    private static final float LINE_WIDTH = 2.0f;

    public static final RenderPipeline LINES_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(MiACompat.MOD_ID, "pipeline/triangulation_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );

    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.CUTOUT_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);

    private final GhostSeekTracker ghostSeekTracker;
    private BufferBuilder buffer;
    private MappableRingBuffer vertexBuffer;

    private boolean overlayEnabled = true;

    public GhostSeekRenderer(GhostSeekTracker ghostSeekTracker) {
        this.ghostSeekTracker = ghostSeekTracker;
    }

    public void toggleOverlay() {
        overlayEnabled = !overlayEnabled;
    }

    public boolean isOverlayEnabled() {
        return overlayEnabled
            && MiACompat.config.ghostSeekBreadcrumbDuration > 0
            && ghostSeekTracker.getGhostSeekType() != null;
    }

    public void render(WorldRenderContext context) {
        if (!isOverlayEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Check if there's anything to render
        if (ghostSeekTracker.getMeasurements().isEmpty()) return;

        // Extraction phase
        extractRenderData(context);

        // Drawing phase
        if (buffer != null) drawThroughWalls(client);
    }

    private void extractRenderData(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getCameraPos();

        assert matrices != null;
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (buffer == null) {
            buffer = new BufferBuilder(allocator, LINES_THROUGH_WALLS.getVertexFormatMode(), LINES_THROUGH_WALLS.getVertexFormat());
        }

        // Render breadcrumbs
        int maxDistance = (ghostSeekTracker.getGhostSeekType() != null) ? ghostSeekTracker.getGhostSeekType().getMaxRange() : 150;
        List<GhostSeekTracker.Measurement> measurements = ghostSeekTracker.getMeasurements();
        for (GhostSeekTracker.Measurement m : measurements) {
            float normalizedDistance = (float) Math.min((m.distance + m.uncertainty) / maxDistance, 1.0);

            VertexRendering.drawBox(matrices, buffer, createCenteredBox(m.position, 0.5), 1.0f, 1.0f - normalizedDistance, 0.0f, 0.6f);
        }

        matrices.pop();
    }

    private Box createCenteredBox(Vec3d center, double size) {
        return createCenteredBox(center, size, size);
    }

    private Box createCenteredBox(Vec3d center, double width, double height) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        return new Box(
            center.x - halfWidth, center.y - halfHeight, center.z - halfWidth,
            center.x + halfWidth, center.y + halfHeight, center.z + halfWidth
        );
    }

    private void drawThroughWalls(MinecraftClient client) {
        BuiltBuffer builtBuffer = buffer.end();
        BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
        draw(client, builtBuffer, drawParameters, vertices, format);

        // Rotate the vertex buffer so we are less likely to use buffers that the GPU is using
        if (vertexBuffer != null) vertexBuffer.rotate();
        buffer = null;
    }

    private GpuBuffer upload(BuiltBuffer.DrawParameters drawParameters, VertexFormat format, BuiltBuffer builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            vertexBuffer = new MappableRingBuffer(() -> RENDER_LABEL,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
            vertexBuffer.getBlocking().slice(0, builtBuffer.getBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.getBuffer(), mappedView.data());
        }

        return vertexBuffer.getBlocking();
    }

    private void draw(MinecraftClient client, BuiltBuffer builtBuffer, BuiltBuffer.DrawParameters drawParameters,
                      GpuBuffer vertices, VertexFormat format) {
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(LINES_THROUGH_WALLS.getVertexFormatMode());
        GpuBuffer indices = shapeIndexBuffer.getIndexBuffer(drawParameters.indexCount());
        VertexFormat.IndexType indexType = shapeIndexBuffer.getIndexType();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, new Vector3f(), RenderSystem.getTextureMatrix(), LINE_WIDTH);

        try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> RENDER_LABEL,
                client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(), OptionalDouble.empty())) {
            renderPass.setPipeline(LINES_THROUGH_WALLS);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            renderPass.drawIndexed(0, 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    public void close() {
        allocator.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}