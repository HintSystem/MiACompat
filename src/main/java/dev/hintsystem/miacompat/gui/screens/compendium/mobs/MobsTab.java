package dev.hintsystem.miacompat.gui.screens.compendium.mobs;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.gui.components.WindowTab;
import dev.hintsystem.miacompat.server.ServerMobRegistry;
import dev.hintsystem.miacompat.server.config.geary.SpawnConfig;
import dev.hintsystem.miacompat.server.config.mythic.mob.MobConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class MobsTab extends WindowTab {
    private static final Component TITLE = Component.translatable("screen.miacompat.compendium.tab.mobs");

    public static final int PAGE_OVERHANG_X = 4;
    public static final int PAGE_OVERHANG_Y = 6;

    public final Map<String, MobSlot> spawnableMobsById = new HashMap<>();

    @Nullable private MobList mobList;
    @Nullable private MobDetailsPage mobDetails;

    public MobsTab() {
        super(TITLE);
    }

    @Override
    public int getContentWidth() { return listWidth() + MobDetailsPage.WIDTH - PAGE_OVERHANG_X - BG_MARGIN; }

    @Override
    public int getContentHeight() { return listHeight(); }

    public int listWidth() { return MobList.containerWidth(6); }
    public int listHeight() { return MobList.containerHeight(6); }

    public ScreenRectangle listRectangle(ScreenRectangle content) {
        return new ScreenRectangle(
            content.left(), content.top(),
            listWidth(), content.height()
        );
    }

    public ScreenRectangle detailsRectangle(ScreenRectangle window) {
        return new ScreenRectangle(
            window.right() - MobDetailsPage.WIDTH + PAGE_OVERHANG_X, window.top(),
            -1, window.height() + PAGE_OVERHANG_Y
        );
    }

    @Override
    public void init(Minecraft minecraft, Font font) {
        for (SpawnConfig spawn : ServerMobRegistry.getAllSpawns()) {
            if (spawn.mobId == null || spawn.regions.isEmpty()) continue;

            MobConfig mob = ServerMobRegistry.getMob(spawn.mobId);
            if (mob == null) {
                MiACompat.LOGGER.warn("Spawnable mob id '{}' not registered", spawn.mobId);
                continue;
            }

            spawnableMobsById.computeIfAbsent(mob.id, k -> new MobSlot(mob))
                .addSpawn(spawn);
        }

        this.mobList = new MobList(
            minecraft, font, spawnableMobsById.values(),
            listRectangle(content)
        );

        this.mobDetails = new MobDetailsPage(
            font,
            this.mobList::getClickedSlot,
            detailsRectangle(window)
        );

        addRenderableWidget(mobList);
        addRenderableWidget(mobDetails);
    }

    @Override
    public void doLayout(ScreenRectangle tabArea) {
        super.doLayout(tabArea);

        if (this.mobList != null)
            this.mobList.setRectangle(listRectangle(content));

        if (this.mobDetails != null)
            this.mobDetails.setRectangle(detailsRectangle(window));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics);
    }
}
