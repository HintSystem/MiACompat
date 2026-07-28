package dev.hintsystem.miacompat.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;

public class WindowTabBar extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
    private static final Component USAGE_NARRATION = Component.translatable("narration.tab_navigation.usage");

    public static final int NO_TAB = -1;
    public static final int MAX_WIDTH = 400;
    public static final int MARGIN = 14;
    public static final int HEIGHT = 24;
    public static final int HEADER_HEIGHT = 2;

    public final LinearLayout layout = LinearLayout.horizontal();

    private final WindowTabManager tabManager;
    private final ImmutableList<WindowTab> tabs;
    private final ImmutableList<TabButton> tabButtons;

    public WindowTabBar(WindowTabManager tabManager, Iterable<WindowTab> tabs) {
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.tabManager = tabManager;

        this.tabs = ImmutableList.copyOf(tabs);
        ImmutableList.Builder<TabButton> builder = ImmutableList.builder();

        for (WindowTab tab : this.tabs) {
            builder.add(this.layout.addChild(new TabButton(tabManager, tab, 0, HEIGHT)));
        }

        this.tabButtons = builder.build();
    }

    /**
     * Selects the tab at the specified index.
     *
     * @param index the index of the tab to select.
     * @param playClickSound whether to play a click sound when selecting the tab.
     */
    public void selectTab(int index, boolean playClickSound) {
        if (this.isFocused()) {
            this.setFocused(this.tabButtons.get(index));
        } else if (this.tabButtons.get(index).isActive()) {
            this.tabManager.setCurrentTab(this.tabs.get(index), playClickSound);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.layout.getX()
            && mouseY >= this.layout.getY()
            && mouseX < this.layout.getX() + this.layout.getWidth()
            && mouseY < this.layout.getY() + this.layout.getHeight();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (this.getFocused() != null) {
            this.setFocused(null);
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        super.setFocused(focused);
        if (focused instanceof TabButton tabButton && tabButton.isActive()) {
            this.tabManager.setCurrentTab(tabButton.tab(), true);
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.isFocused()) {
            TabButton tabButton = this.currentTabButton();
            if (tabButton != null) {
                return ComponentPath.path(this, ComponentPath.leaf(tabButton));
            }
        }

        return event instanceof FocusNavigationEvent.TabNavigation ? null : super.nextFocusPath(event);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.tabButtons;
    }

    public List<WindowTab> getTabs() {
        return this.tabs;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return this.layout.getRectangle();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (TabButton tabButton : this.tabButtons) {
            tabButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Arranges the elements within the tabbed layout.
     */
    public void arrangeElements() {
        ScreenRectangle windowArea = tabManager.getWindowArea();
        if (windowArea == null) return;

        int barWidth = Math.min(MAX_WIDTH, windowArea.width()) - MARGIN*2;
        int buttonWidth = Mth.roundToward(barWidth / this.tabButtons.size(), 2);

        for (TabButton tabButton : this.tabButtons) {
            tabButton.setWidth(buttonWidth);
        }

        this.layout.setPosition(
            windowArea.left() + Mth.roundToward((windowArea.width() - barWidth) / 2, 2),
            windowArea.top() - HEIGHT + HEADER_HEIGHT
        );
        this.layout.arrangeElements();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.hasControlDownWithQuirk()) {
            int i = this.getNextTabIndex(event);
            if (i != NO_TAB) {
                this.selectTab(Mth.clamp(i, 0, this.tabs.size() - 1), true);
                return true;
            }
        }

        return false;
    }

    private int getNextTabIndex(KeyEvent event) {
        return this.getNextTabIndex(this.currentTabIndex(), event);
    }

    private int getNextTabIndex(int tabIndex, KeyEvent event) {
        int i = event.getDigit();
        if (i != NO_TAB) {
            return Math.floorMod(i - 1, 10);
        } else if (event.isCycleFocus() && tabIndex != NO_TAB) {
            int j = event.hasShiftDown() ? tabIndex - 1 : tabIndex + 1;
            int k = Math.floorMod(j, this.tabs.size());
            return this.tabButtons.get(k).active ? k : this.getNextTabIndex(k, event);
        } else {
            return NO_TAB;
        }
    }

    /**
     * Returns the index of the current tab.
     * <p>
     * @return the index of the current tab, or -1 if no current tab is set.
     */
    public int currentTabIndex() {
        Tab tab = this.tabManager.getCurrentTab();
        return this.tabs.indexOf(tab);
    }

    /**
     * Returns the current tab button.
     * <p>
     * @return the current tab button, or null if no current tab is set.
     */
    private @Nullable TabButton currentTabButton() {
        int i = this.currentTabIndex();
        return i != NO_TAB ? this.tabButtons.get(i) : null;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.tabButtons.stream().map(AbstractWidget::narrationPriority).max(Comparator.naturalOrder()).orElse(NarratableEntry.NarrationPriority.NONE);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        Optional<TabButton> optional = this.tabButtons.stream().filter(AbstractWidget::isHovered).findFirst().or(() -> Optional.ofNullable(this.currentTabButton()));
        optional.ifPresent(tabButton -> {
            this.narrateListElementPosition(narrationElementOutput.nest(), tabButton);
            tabButton.updateNarration(narrationElementOutput);
        });
        if (this.isFocused()) {
            narrationElementOutput.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }
    }

    /**
     * Narrates the position of a list element (tab button).
     *
     * @param narrationElementOutput the narration output to update.
     * @param tabButton the tab button whose position is being narrated.
     */
    protected void narrateListElementPosition(NarrationElementOutput narrationElementOutput, TabButton tabButton) {
        if (this.tabs.size() > 1) {
            int i = this.tabButtons.indexOf(tabButton);
            if (i != -1) {
                narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.tab", i + 1, this.tabs.size()));
            }
        }
    }
}
