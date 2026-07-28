package dev.hintsystem.miacompat.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

public class WindowTabManager extends TabManager {
    protected ScreenRectangle tabArea;
    Runnable updateTabLayout;

    public WindowTabManager(
        Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget,
        Runnable updateTabLayout
    ) {
        super(addWidget, removeWidget);
        this.updateTabLayout = updateTabLayout;
    }

    public void initTabs(
        List<WindowTab> tabs,
        Minecraft minecraft, Font font, int initialTabIndex
    ) {
        if (tabArea == null)
            throw new IllegalArgumentException("Tab area must not be null before initializing tabs");

        for (WindowTab tab : tabs) {
            tab.updateWindowBounds(tabArea);
            tab.init(minecraft, font);
        }

        setCurrentTab(tabs.get(initialTabIndex), false);
    }

    @Override
    public void setTabArea(ScreenRectangle tabArea) {
        super.setTabArea(tabArea);
        this.tabArea = tabArea;
    }

    @Override
    public void setCurrentTab(Tab tab, boolean playClickSound) {
        super.setCurrentTab(tab, playClickSound);
        updateTabLayout.run();
    }

    public @Nullable WindowTab getCurrentWindowTab() {
        Tab tab = this.getCurrentTab();
        return tab instanceof WindowTab windowTab ? windowTab : null;
    }

    public ScreenRectangle getTabArea() { return tabArea; }

    public ScreenRectangle getWindowArea() {
        WindowTab tab = getCurrentWindowTab();
        return tab != null ? tab.window : tabArea;
    }
}
