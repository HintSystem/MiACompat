package dev.hintsystem.miacompat.gui.screens.compendium;

import dev.hintsystem.miacompat.client.KeyBindings;
import dev.hintsystem.miacompat.gui.components.WindowTab;
import dev.hintsystem.miacompat.gui.components.WindowTabBar;
import dev.hintsystem.miacompat.gui.components.WindowTabManager;
import dev.hintsystem.miacompat.gui.screens.compendium.mobs.MobsTab;
import dev.hintsystem.miacompat.gui.screens.compendium.relics.RelicsTab;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

public class CompendiumScreen extends Screen {
    private static final int MARGIN_Y = 64;
    private static final int MIN_HEIGHT = 160;
    private static final int MAX_HEIGHT = 400;

    private final HeaderAndFooterLayout layout;

    private final WindowTabManager tabManager = new WindowTabManager(
        this::addRenderableWidget, this::removeWidget,
        this::arrangeTabBar
    );
    private final WindowTabBar tabBar;

    private final CompendiumTabId initialTab;

    public enum CompendiumTabId {
        MOBS(MobsTab::new, KeyBindings.OPEN_MOB_COMPENDIUM),
        RELICS(RelicsTab::new, KeyBindings.OPEN_RELIC_COMPENDIUM);

        public final Supplier<WindowTab> factory;
        @Nullable public final KeyMapping mapping;

        CompendiumTabId(Supplier<WindowTab> factory, KeyMapping mapping) {
            this.factory = factory;
            this.mapping = mapping;
        }

        WindowTab create() { return factory.get(); }
    }

    public CompendiumScreen(CompendiumTabId initialTab) {
        super(Component.translatable("screen.miacompat.compendium.title"));
        this.layout = new HeaderAndFooterLayout(this);

        this.tabBar = new WindowTabBar(tabManager,
            Arrays.stream(CompendiumTabId.values())
                .map(CompendiumTabId::create)
                .toList()
        );

        this.initialTab = initialTab;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);

        this.addWidget(tabBar);

        this.layout.visitWidgets(widget -> {
            widget.setTabOrderGroup(1);
            this.addRenderableWidget(widget);
        });

        this.repositionElements();

        this.tabManager.initTabs(
            tabBar.getTabs(),
            minecraft, font, initialTab.ordinal()
        );
    }

    @Override
    public void repositionElements() {
        int top = MARGIN_Y;
        int height = Math.max(MIN_HEIGHT, this.height - MARGIN_Y*2);
        if (height > MAX_HEIGHT) {
            height = MAX_HEIGHT;
            top = (this.height - MAX_HEIGHT) / 2;
        }

        this.tabManager.setTabArea(new ScreenRectangle(
            0, top, this.width, height
        ));

        arrangeTabBar();
        this.layout.arrangeElements();
    }

    public void arrangeTabBar() {
        this.tabBar.arrangeElements();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.tabBar.render(guiGraphics, mouseX, mouseY, partialTick);

        WindowTab windowTab = this.tabManager.getCurrentWindowTab();
        if (windowTab != null) windowTab.render(guiGraphics, mouseX, mouseY, partialTick);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }

        for (CompendiumTabId tab : CompendiumTabId.values()) {
            if (tab.mapping == null) continue;
            if (!tab.mapping.matches(event)) continue;

            if (tabBar.currentTabIndex() == tab.ordinal()) {
                this.onClose();
            } else {
                this.tabBar.selectTab(tab.ordinal(), true);
            }

            return true;
        }

        return false;
    }
}
