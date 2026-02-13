package dev.hintsystem.miacompat.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public class Hud implements HudElement {

    private final GhostSeekCooldown ghostSeekCooldown;

    public Hud() {
        ghostSeekCooldown = new GhostSeekCooldown();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        ghostSeekCooldown.render(guiGraphics, deltaTracker);
    }
}
