package dev.hintsystem.miacompat.gui.screens.compendium.relics;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.gui.components.WindowTab;
import dev.hintsystem.miacompat.server.ServerItemRegistry;
import dev.hintsystem.miacompat.server.ServerMobRegistry;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicGrade;
import dev.hintsystem.miacompat.server.config.mythic.drop.ItemDrop;
import dev.hintsystem.miacompat.server.config.mythic.drop.MobDrop;
import dev.hintsystem.miacompat.server.config.mythic.drop.RelicLayer;
import dev.hintsystem.miacompat.server.config.mythic.mob.MobConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;

import org.jetbrains.annotations.Nullable;

public class RelicsTab extends WindowTab {
    private static final Component TITLE = Component.translatable("screen.miacompat.compendium.tab.relics");

    @Nullable private RelicList relicList;

    public final EnumMap<RelicGrade, List<RelicSlot>> relicsByGrade = new EnumMap<>(RelicGrade.class);
    public final Map<Identifier, List<MobDrop<ItemDrop>>> relicDropByPrefabId = new HashMap<>();

    private static final Comparator<RelicSlot> RELIC_SORTER = Comparator
        .comparingInt(RelicSlot::layerOrder)
        .thenComparingInt(r -> r.config.name.getString()
            .toLowerCase(Locale.ROOT)
            .contains("ghost seek") ? 0 : 1)
        .thenComparing(r -> r.config.name.getString());

    public RelicsTab() {
        super(TITLE);
    }

    @Override
    public int getWindowWidth() { return listWidth() + BG_MARGIN*2; }

    public int listWidth() { return RelicList.containerWidth(10); }

    public ScreenRectangle listRectangle(ScreenRectangle content) {
        return new ScreenRectangle(
            content.left(), content.top(),
            listWidth(), content.height()
        );
    }

    @Override
    public void init(Minecraft minecraft, Font font) {
        loadRelics();

        this.relicList = new RelicList(
            minecraft, font, this.relicsByGrade,
            listRectangle(content)
        );

        addRenderableWidget(relicList);
    }

    @Override
    public void doLayout(ScreenRectangle tabArea) {
        super.doLayout(tabArea);

        if (this.relicList != null)
            this.relicList.setRectangle(listRectangle(content));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics);
    }

    public void loadRelics() {
        relicDropByPrefabId.clear();
        relicsByGrade.clear();

        for (RelicLayer relicLayer : RelicLayer.values()) {
            MobConfig relicDropMob = ServerMobRegistry.getMob(relicLayer.mobId);
            if (relicDropMob == null) {
                MiACompat.LOGGER.warn("Relic drop mob id '{}' not registered", relicLayer.mobId);
                continue;
            }

            // Using resolveDrops instead of resolveRelicDrops, because sun spheres do not use relic drop skill
            for (var mobDrop : ServerMobRegistry.resolveDrops(relicDropMob)) {
                if (!(mobDrop.drop() instanceof ItemDrop itemDrop)) continue;

                MobDrop<ItemDrop> itemMobDrop = mobDrop.withDrop(itemDrop);

                relicDropByPrefabId
                    .computeIfAbsent(itemDrop.itemId, (k) -> new ArrayList<>())
                    .add(itemMobDrop);
            }
        }

        for (ItemConfig itemConfig : ServerItemRegistry.getAllItems().values()) {
            if (!(itemConfig instanceof RelicConfig relicConfig)) continue;

            var drops = relicDropByPrefabId.get(relicConfig.prefabId);

            relicsByGrade
                .computeIfAbsent(relicConfig.grade, (k) -> new ArrayList<>())
                .add(new RelicSlot(relicConfig, drops));
        }

        for (List<RelicSlot> relics : relicsByGrade.values()) {
            relics.sort(RELIC_SORTER);
        }
    }
}
