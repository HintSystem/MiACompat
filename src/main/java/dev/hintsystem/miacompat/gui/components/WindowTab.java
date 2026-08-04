package dev.hintsystem.miacompat.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class WindowTab implements Tab, Renderable {
    protected static final Identifier BG_SPRITE = Identifier.withDefaultNamespace("social_interactions/background");
    protected static final int BG_MARGIN = 8;

    private final Component title;

    protected ScreenRectangle window;
    protected ScreenRectangle content;

    private final List<AbstractWidget> widgets = new ArrayList<>();

    public WindowTab(Component title) {
        this.title = title;
    }

    @Override
    public Component getTabTitle() {
        return this.title;
    }

    public abstract int getContentWidth();
    public int getContentHeight() { return -1; }

    public abstract void init(Minecraft minecraft, Font font);

    protected void addRenderableWidget(AbstractWidget widget) {
        widgets.add(widget);
    }

    @Override
    public void visitChildren(Consumer<AbstractWidget> consumer) {
        widgets.forEach(consumer);
    }

    public void updateWindowBounds(ScreenRectangle tabArea) {
        int contentHeight = Math.min(tabArea.height(), getContentHeight());

        int windowWidth = getContentWidth() + BG_MARGIN*2;
        int windowHeight = contentHeight != -1 ? contentHeight : tabArea.height();
        int windowX = tabArea.left() + Math.floorDiv(tabArea.width() - windowWidth, 2);

        this.window = new ScreenRectangle(
            windowX, tabArea.top(),
            windowWidth, windowHeight
        );

        this.content = new ScreenRectangle(
            window.left() + BG_MARGIN, window.top() + BG_MARGIN,
            window.width() - BG_MARGIN*2, window.height() - BG_MARGIN*2
        );
    }

    @Override
    public void doLayout(ScreenRectangle tabArea) {
        updateWindowBounds(tabArea);
    }

    public abstract void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    protected void renderBackground(GuiGraphics guiGraphics) {
        if (window == null) return;

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BG_SPRITE, window.left(), window.top(), window.width(), window.height());
    }

    @Override
    public Component getTabExtraNarration() { return Component.empty(); }
}
