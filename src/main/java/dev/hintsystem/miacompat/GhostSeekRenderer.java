package dev.hintsystem.miacompat;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class GhostSeekRenderer {
    private static final String RENDER_LABEL = MiACompat.MOD_ID + " ghost seek renderer";
    private static final float LINE_WIDTH = 4.0f;

    public static final RenderPipeline LINES_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines_no_fog")
            .withBlend(BlendFunction.TRANSLUCENT).withCull(false).withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .withLocation(Identifier.fromNamespaceAndPath(MiACompat.MOD_ID, "pipeline/triangulation_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );

    private static final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private BufferBuilder buffer;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private MappableRingBuffer vertexBuffer;

    private final GhostSeekTracker ghostSeekTracker;

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

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Check if there's anything to render
        if (ghostSeekTracker.getMeasurements().isEmpty()) return;

        // Extraction phase
        extractRenderData(context);

        // Drawing phase
        if (buffer != null) drawThroughWalls(client);
    }

    private void extractRenderData(WorldRenderContext context) {
        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (buffer == null) {
            buffer = new BufferBuilder(allocator, LINES_THROUGH_WALLS.getVertexFormatMode(), LINES_THROUGH_WALLS.getVertexFormat());
        }

        // Render breadcrumbs
        int maxDistance = (ghostSeekTracker.getGhostSeekType() != null) ? ghostSeekTracker.getGhostSeekType().getMaxRange() : 150;
        List<GhostSeekTracker.Measurement> measurements = ghostSeekTracker.getMeasurements();
        for (GhostSeekTracker.Measurement m : measurements) {
            double distance = (m.distance - m.uncertainty > 0) ?
                 m.distance + m.uncertainty : 0;

            float normalizedDistance = (float) Math.min(distance / maxDistance, 1.0);

            float red, green;
            if (normalizedDistance < 0.5f) {
                green = 1.0f;
                red = normalizedDistance * 2.0f;
            } else {
                green = 1.0f - (normalizedDistance - 0.5f) * 2.0f;
                red = 1.0f;
            }

            int color = ARGB.colorFromFloat(0.6f, red, green, 0);

            double cameraDistance = camera.distanceTo(m.position);
            float scaledLineWidth = Math.max(LINE_WIDTH * (float)(6 / cameraDistance), 1.5f);

            ShapeRenderer.renderShape(matrices, buffer, createCenteredBox(m.position, 0.5),
                0, 0, 0, color, scaledLineWidth);
        }

        matrices.popPose();
    }

    private VoxelShape createCenteredBox(Vec3 center, double size) {
        return createCenteredBox(center, size, size);
    }

    private VoxelShape createCenteredBox(Vec3 center, double width, double height) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        return Shapes.box(
            center.x - halfWidth, center.y - halfHeight, center.z - halfWidth,
            center.x + halfWidth, center.y + halfHeight, center.z + halfWidth
        );
    }

    private void drawThroughWalls(Minecraft client) {
        MeshData builtBuffer = buffer.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
        draw(client, LINES_THROUGH_WALLS, builtBuffer, drawParameters, vertices, format);

        // Rotate the vertex buffer so we are less likely to use buffers that the GPU is using
        if (vertexBuffer != null) vertexBuffer.rotate();
        buffer = null;
    }

    private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) vertexBuffer.close();

            vertexBuffer = new MappableRingBuffer(() -> RENDER_LABEL,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        return vertexBuffer.currentBuffer();
    }

    private void draw(Minecraft client, RenderPipeline pipeline, MeshData builtBuffer, MeshData.DrawState drawParameters,
                      GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            // Sort the quads if there is translucency
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
            // Upload the index buffer
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            // Use the general shape index buffer for non-quad draw modes
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        // Actually execute the draw
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> RENDER_LABEL, client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // The base vertex is the starting index when we copied the data into the vertex buffer divided by vertex size
            //noinspection ConstantValue
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
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