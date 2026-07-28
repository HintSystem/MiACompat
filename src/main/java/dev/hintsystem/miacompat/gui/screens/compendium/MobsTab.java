package dev.hintsystem.miacompat.gui.screens.compendium;

import dev.hintsystem.miacompat.gui.components.WindowTab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public class MobsTab extends WindowTab {
    private static final Component TITLE = Component.translatable("screen.miacompat.compendium.tab.mobs");

    public MobsTab() {
        super(TITLE);
    }

    @Override
    public int getWindowWidth() { return 400; }

    @Override
    public void init(Minecraft minecraft, Font font) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics);
    }

    @Override
    public void doLayout(ScreenRectangle tabArea) {
        super.doLayout(tabArea);
    }
}
