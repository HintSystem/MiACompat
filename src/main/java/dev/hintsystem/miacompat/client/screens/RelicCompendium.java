package dev.hintsystem.miacompat.client.screens;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.KeyBindings;
import dev.hintsystem.miacompat.client.MiaIcons;
import dev.hintsystem.miacompat.server.ServerItemRegistry;
import dev.hintsystem.miacompat.server.ServerMobRegistry;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicGrade;
import dev.hintsystem.miacompat.server.config.mythic.drop.ItemDrop;
import dev.hintsystem.miacompat.server.config.mythic.drop.MobDrop;
import dev.hintsystem.miacompat.server.config.mythic.drop.RelicLayer;
import dev.hintsystem.miacompat.server.config.mythic.mob.MobConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;

import org.jetbrains.annotations.Nullable;

public class RelicCompendium extends Screen {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("social_interactions/background");
    private static final int BG_MARGIN = 8;

    private final HeaderAndFooterLayout layout;

    public final EnumMap<RelicGrade, List<Relic>> relicsByGrade = new EnumMap<>(RelicGrade.class);
    public final Map<Identifier, List<MobDrop<ItemDrop>>> relicDropByPrefabId = new HashMap<>();

    private static final Comparator<Relic> RELIC_SORTER = Comparator
        .comparingInt(Relic::layerOrder)
        .thenComparingInt(r -> r.config.name.getString()
            .toLowerCase(Locale.ROOT)
            .contains("ghost seek") ? 0 : 1)
        .thenComparing(r -> r.config.name.getString());

    public RelicCompendium() {
        super(Component.translatable("screen.miacompat.relic_compendium"));
        this.layout = new HeaderAndFooterLayout(this);
    }

    public int windowX() { return (this.width - windowWidth()) / 2; }
    public int windowY() { return 64; }
    public int windowWidth() { return listWidth() + BG_MARGIN*2; }
    public int windowHeight() { return Math.max(52, this.height - 128); }

    public int listX() { return windowX() + BG_MARGIN; }
    public int listY() { return windowY() + BG_MARGIN; }
    public int listWidth() { return RelicList.containerWidth(10); }
    public int listHeight() { return windowHeight() - BG_MARGIN*2; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;

        if (KeyBindings.OPEN_RELIC_COMPENDIUM.matches(event)) {
            this.onClose();
            return true;
        }

        return false;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);

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
                .add(new Relic(relicConfig, drops));
        }

        for (List<Relic> relics : relicsByGrade.values()) {
            relics.sort(RELIC_SORTER);
        }

        this.addRenderableWidget(new RelicList(
            this.minecraft, this.font, this.relicsByGrade,
            listX(), listY(), listWidth(), listHeight()
        ));

        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.arrangeElements();
    }

    public static class Relic {
        public final RelicConfig config;
        public final List<MobDrop<ItemDrop>> drops;
        public final ItemStack item;

        public final int borderColor;

        public Relic(RelicConfig config, @Nullable List<MobDrop<ItemDrop>> drops) {
            this.config = config;
            this.drops = drops != null ? drops : List.of();
            this.item = new ItemStack(config.type);

            TextColor textColor = config.grade.displayName.getStyle().getColor();
            if (config.lore.size() >= 2) {
                textColor = config.lore.get(1).getStyle().getColor();
            }

            this.borderColor = textColor != null ? textColor.getValue() : 0;

            List<Component> lore = new ArrayList<>(config.lore);
            lore.set(0, Component.literal(
                config.grade.displayName.getString() + " Relic"
            ));

            item.set(DataComponents.CUSTOM_NAME, config.name);
            item.set(DataComponents.ITEM_MODEL, config.modelId);
            item.set(DataComponents.LORE, new ItemLore(lore));
        }

        public boolean isUnlocked() { return false; }

        public boolean isHidden() { return false && !drops.isEmpty(); }

        private int layerOrder() {
            if (drops.isEmpty()) return Integer.MIN_VALUE;

            return RelicLayer.fromMobDrop(drops.getFirst())
                .map(Enum::ordinal)
                .orElse(Integer.MIN_VALUE);
        }

        public List<Component> getTooltip(Minecraft minecraft) {
            MutableComponent dropChances = Component.empty();
            for (var mobDrop : drops) {
                RelicLayer.fromMobDrop(mobDrop).ifPresent((l) -> {
                    AtlasSprite sprite = MiaIcons.getLayerSprite(l.info.iconName);

                    dropChances.append(Component.object(sprite)).append(" ");
                });

                dropChances.append(
                    Component.literal(
                        String.format(Locale.ROOT, "%.3f", mobDrop.drop().chance * 100)
                            .replaceAll("\\.?0+$", "")
                    ).append("% ").withStyle(ChatFormatting.GRAY));
            }

            if (isHidden()) {
                return List.of(
                    Component.literal("???").setStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY).withItalic(true)),
                    dropChances
                );
            }

            List<Component> tooltip = Screen.getTooltipFromItem(minecraft, item);
            if (!dropChances.equals(Component.empty()))
                tooltip.add(1, dropChances);

            return tooltip;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, windowX(), windowY(), windowWidth(), windowHeight());
    }
}
