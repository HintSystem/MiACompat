package dev.hintsystem.miacompat.gui;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.mixin.GuiGraphicsAccessor;
import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;

import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.NotNull;

public class CurseMeter implements HudElement {
    private static final Identifier BAR_BACKGROUND = MiACompat.id("textures/gui/curse_background.png");
    private static final int BAR_BACKGROUND_WIDTH = 27;
    private static final int BAR_BACKGROUND_HEIGHT = 89;

    private static final Identifier BAR = MiACompat.id("textures/gui/curse_bar.png");
    private static final int BAR_WIDTH = 9;
    private static final int BAR_HEIGHT = 72;

    public static final int CURSE_HEIGHT_GAIN = 10; // it takes going up this amount of blocks before being affected by the curse

    private int animationTicks = 0;
    private int curseTicks = 0;

    public int curseStacks = 0;
    public int curseAccrued = 0;
    private BlockPos lastPlayerPos = BlockPos.ZERO;
    private MiaDeeperWorld.LayerInfo currentLayer = MiaDeeperWorld.LayerInfo.Orth;

    protected void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (curseAccrued >= 3 || curseStacks > 0) { animationTicks++; } else { animationTicks = 0; }
        if (curseTicks > 0) { curseTicks--; } else { curseStacks = 0; }

        int changeY = player.blockPosition().getY() - lastPlayerPos.getY();
        double changeDistance = player.blockPosition().distSqr(lastPlayerPos);

        lastPlayerPos = player.blockPosition();
        currentLayer = MiaDeeperWorld.LayerInfo.fromUnwrappedY(MiaDeeperWorld.unwrap(lastPlayerPos).getY());
        if (changeDistance > 32 * 32) return;

        int totalCurse = Math.max(curseAccrued + changeY, 0);
        int curseStackGain = Math.floorDiv(totalCurse, CURSE_HEIGHT_GAIN);
        curseAccrued = totalCurse % CURSE_HEIGHT_GAIN;
        curseStacks += curseStackGain;

        if (curseStackGain > 0) { curseTicks = 200; }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        if (currentLayer.ascensionEffects.isEmpty()) return;

        int xPos = guiGraphics.guiWidth() - BAR_BACKGROUND_WIDTH*2 - 10;
        int yPos = guiGraphics.guiHeight() - BAR_BACKGROUND_HEIGHT - 15;

        guiGraphics.blit(BAR_BACKGROUND,
            xPos, yPos,
            xPos + BAR_BACKGROUND_WIDTH, yPos + BAR_BACKGROUND_HEIGHT,
            0f, 1f,
            0f, 1f
        );

        int barX = xPos + 4;
        int barY = yPos + BAR_BACKGROUND_HEIGHT - BAR_HEIGHT - 2;

        renderSoulSprite(guiGraphics, barX - 3, yPos + 1);

        int effectX = xPos + BAR_BACKGROUND_WIDTH - 14;
        int effectY = yPos + 3;
        for (Holder<@NotNull MobEffect> mobEffectHolder : currentLayer.ascensionEffects) {
            Identifier effectTexture = Gui.getMobEffectSprite(mobEffectHolder);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, effectTexture,
                effectX, effectY, 12, 12, ARGB.color(0.4f, CommonColors.WHITE));
            effectY += 13;
        }

        if (curseStacks > 0) {
            renderCurseStacks(guiGraphics, xPos + BAR_BACKGROUND_WIDTH - 1, yPos + BAR_BACKGROUND_HEIGHT - 10);
        }

        int barCapSize = 6;
        int filledBarHeight = BAR_HEIGHT - barCapSize*2;
        float progress = Math.clamp((float) curseAccrued / CURSE_HEIGHT_GAIN, 0f, 1f);
        int fillBarProgress = (int) (progress * filledBarHeight);
        int fillBarY = barY + barCapSize + (filledBarHeight - fillBarProgress);

        guiGraphics.blit(BAR,
            barX, barY,
            barX + BAR_WIDTH, barY + BAR_HEIGHT,
            0, 1, 0, 1
        );

        // Secondary fill bar
        if (curseStacks > 0) {
            int previousColor = getBarColor(curseStacks - 1);
            guiGraphics.fill(
                barX+2, barY + barCapSize,
                barX-2 + BAR_WIDTH, barY + barCapSize + filledBarHeight,
                previousColor
            );
        }

        // Main fill bar
        guiGraphics.fill(
            barX+2, fillBarY,
            barX-2 + BAR_WIDTH, fillBarY + fillBarProgress,
            getBarColor(curseStacks)
        );

        var pose = guiGraphics.pose().pushMatrix();
        pose.translate(barX+2, fillBarY + fillBarProgress);
        pose.rotate((float) Math.toRadians(-90));

        // Multiply overlay (rotated from horizontal)
        guiGraphics.blit(Hud.GUI_TEXTURED_MULTIPLY, Hud.BAR_OVERLAY,
            0, 0,
            0, 0,
            curseStacks == 0 ? fillBarProgress : filledBarHeight, 5,
            Hud.BAR_OVERLAY_WIDTH, Hud.BAR_OVERLAY_HEIGHT
        );

        pose.popMatrix();

        guiGraphics.fill(
            barX+1, fillBarY,
            barX-1 + BAR_WIDTH, fillBarY + 1,
            ARGB.color(0.35f, CommonColors.WHITE)
        );
    }

    private int getBarColor(int stack) {
        int cycle = stack % 5; // Loop every 5 stacks

        return switch (cycle) {
            case 0 -> ARGB.color(180, 120, 220); // Light purple
            case 1 -> ARGB.color(200, 80, 240); // Vibrant
            case 2 -> ARGB.color(180, 60, 200); // Dark
            case 3 -> ARGB.color(150, 40, 170); // Dark vibrant
            case 4 -> ARGB.color(110, 25, 130); // Darkest purple
            default -> ARGB.color(180, 120, 220); // Fallback
        };
    }

    private void renderCurseStacks(GuiGraphics guiGraphics, int x, int y) {
        ActiveTextCollector textRenderer = guiGraphics.textRenderer();
        Style textStyle = Style.EMPTY.withShadowColor(CommonColors.BLACK);

        textRenderer.accept(TextAlignment.RIGHT, x, y,
            Component.literal(String.valueOf(curseStacks)).withStyle(textStyle));

        textRenderer.accept(TextAlignment.RIGHT, x, y - 9,
            textRenderer.defaultParameters().withOpacity(0.5f), Component.literal("x").withStyle(textStyle));
    }

    private void renderSoulSprite(GuiGraphics guiGraphics, int x, int y) {
        int animationTicks = Math.max(this.animationTicks, 5) - 5; // do not animate first 5 ticks

        Minecraft client = Minecraft.getInstance();
        TextureAtlas particleAtlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);

        int frameToUse;
        boolean flipHorizontal = false;
        boolean flipVertical = false;

        if (curseAccrued <= 7 && curseStacks == 0) {
            int totalFrames = 5; // 2,3,4 flipped 3 . . .
            int animationIndex = ((animationTicks / 4) % totalFrames) + 1;

            if (animationIndex == 5) {
                frameToUse = 3;
                flipHorizontal = true;
                flipVertical = true;
            } else { frameToUse = animationIndex; }
        } else {
            int totalFrames = 5; // 4,5,6,7
            int animationIndex = ((animationTicks / 4) % totalFrames) + 4;

            if (animationIndex == 8) {
                frameToUse = 6;
                flipHorizontal = true;
                flipVertical = true;
            } else { frameToUse = animationIndex; }
        }

        TextureAtlasSprite sprite = particleAtlas.getSprite(Identifier.withDefaultNamespace("soul_" + frameToUse));

        renderFlippedSprite(guiGraphics, sprite,
            x-1, y-1, 16, 16,
            flipHorizontal, flipVertical, 0xffc947ff);
        renderFlippedSprite(guiGraphics, sprite,
            x+1, y+1, 16, 16,
            flipHorizontal, flipVertical, 0xffc947ff);

        renderFlippedSprite(guiGraphics, sprite,
            x, y, 16, 16,
            flipHorizontal, flipVertical, -1);
    }

    private void renderFlippedSprite(GuiGraphics guiGraphics, TextureAtlasSprite sprite,
                                     int x, int y, int width, int height,
                                     boolean flipHorizontal, boolean flipVertical, int color) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        if (flipHorizontal) {
            float temp = u0;
            u0 = u1;
            u1 = temp;
        }

        if (flipVertical) {
            float temp = v0;
            v0 = v1;
            v1 = temp;
        }

        //guiGraphics.blit(sprite.atlasLocation(), x, y, x+width, y+height, u0, u1, v0, v1);

        ((GuiGraphicsAccessor) guiGraphics).
            miacompat$innerBlit(RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(),
                x, x+width,
                y, y + height,
                u0, u1,
                v0, v1,
                color
            );
    }
}
