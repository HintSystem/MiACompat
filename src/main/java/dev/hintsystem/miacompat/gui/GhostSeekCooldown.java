package dev.hintsystem.miacompat.gui;

import dev.hintsystem.miacompat.GhostSeekTracker;
import dev.hintsystem.miacompat.MiACompat;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GhostSeekCooldown implements HudElement {
    private static final Identifier BAR_BACKGROUND = MiACompat.id("textures/gui/cooldown_bar.png");
    private static final int BAR_BACKGROUND_WIDTH = 194;
    private static final int BAR_BACKGROUND_HEIGHT = 11;

    private Integer lastColor;

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        GhostSeekTracker ghostSeekTracker = MiACompat.ghostSeekTracker;
        GhostSeekTracker.GhostSeekType ghostSeekType = ghostSeekTracker.getLastGhostSeekType();
        List<GhostSeekTracker.Measurement> measurements = ghostSeekTracker.getMeasurements();

        if (!measurements.isEmpty()) {
            lastColor = measurements.getLast().getColor(ghostSeekTracker.getMaxRange());
        }

        if (ghostSeekType == null) { ghostSeekType = GhostSeekTracker.GhostSeekType.REFINED; }
        if (lastColor == null) return;

        float maxCooldown = ghostSeekType.pingIntervalTicks;
        int cooldown = ghostSeekTracker.awaitingPingTicks;

        if (cooldown == 0) return;

        int xPos = guiGraphics.guiWidth() / 2;
        int yPos = guiGraphics.guiHeight() - 52;
        int bgY = yPos - BAR_BACKGROUND_HEIGHT;
        int filledY = bgY + 3;

        float progress = cooldown / maxCooldown;
        int halfWidth = (int) (progress * Hud.BAR_OVERLAY_WIDTH);

        int bgCapWidth = 6;
        int bgHalfWidth = halfWidth + bgCapWidth;

        float fadeInPortion = 0.02f;
        float fadeOutPortion = 0.1f;

        float alpha;
        if (progress > 1f - fadeInPortion) {
            float t = (1f - progress) / fadeInPortion;
            alpha = Math.clamp(t, 0f, 1f);
        } else if (progress < fadeOutPortion) {
            float t = progress / fadeOutPortion;
            alpha = Math.clamp(t, 0f, 1f);
        } else {
            alpha = 1f;
        }

        int colorAlpha = ARGB.color(alpha, lastColor);
        int whiteAlpha = ARGB.color(alpha, -1);

        // Left side cap and bar outline
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND,
            xPos - bgHalfWidth, bgY,
            0, 0,
            bgHalfWidth, BAR_BACKGROUND_HEIGHT,
            BAR_BACKGROUND_WIDTH, BAR_BACKGROUND_HEIGHT,
            whiteAlpha
        );

        // Right side cap and bar outline
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND,
            xPos, bgY,
            BAR_BACKGROUND_WIDTH - bgHalfWidth, 0,
            bgHalfWidth, BAR_BACKGROUND_HEIGHT,
            BAR_BACKGROUND_WIDTH, BAR_BACKGROUND_HEIGHT,
            whiteAlpha
        );

        guiGraphics.enableScissor(
            xPos - halfWidth, filledY-1,
            xPos + halfWidth, filledY+1 + Hud.BAR_OVERLAY_HEIGHT
        );

        guiGraphics.fill(xPos - halfWidth, filledY, xPos + halfWidth, filledY + Hud.BAR_OVERLAY_HEIGHT, colorAlpha);

        // Right side
        guiGraphics.blit(Hud.GUI_TEXTURED_MULTIPLY, Hud.BAR_OVERLAY,
            xPos, filledY,
            0, 0,
            Hud.BAR_OVERLAY_WIDTH, Hud.BAR_OVERLAY_HEIGHT,
            Hud.BAR_OVERLAY_WIDTH, Hud.BAR_OVERLAY_HEIGHT,
            whiteAlpha
        );

        // Left side
        guiGraphics.blit(Hud.GUI_TEXTURED_MULTIPLY, Hud.BAR_OVERLAY,
            xPos - Hud.BAR_OVERLAY_WIDTH, filledY,
            0, 0,
            Hud.BAR_OVERLAY_WIDTH, Hud.BAR_OVERLAY_HEIGHT,
            -Hud.BAR_OVERLAY_WIDTH, Hud.BAR_OVERLAY_HEIGHT,
            whiteAlpha
        );

        guiGraphics.disableScissor();
    }
}
