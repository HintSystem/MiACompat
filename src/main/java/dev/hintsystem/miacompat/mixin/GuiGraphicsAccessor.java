package dev.hintsystem.miacompat.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Invoker("innerBlit")
    void miacompat$innerBlit(final RenderPipeline renderPipeline,
                                final Identifier location,
                                final int x0,
                                final int x1,
                                final int y0,
                                final int y1,
                                final float u0,
                                final float u1,
                                final float v0,
                                final float v1,
                                final int color);
}
