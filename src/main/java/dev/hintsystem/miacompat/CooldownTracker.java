package dev.hintsystem.miacompat;

import dev.hintsystem.miacompat.server.schema.ItemConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

public class CooldownTracker {
    private static final Map<Identifier, GearCooldown> itemCooldownConfigs = new HashMap<>();
    private static int cooldownTicks = 0;

    public static class GearCooldown {
        @Nullable public final ActionCooldown leftClick;
        @Nullable public final ActionCooldown rightClick;

        public GearCooldown(@Nullable ActionCooldown leftClick, @Nullable ActionCooldown rightClick) {
            this.leftClick = leftClick;
            this.rightClick = rightClick;
        }

        @Nullable
        public static GearCooldown fromItemConfig(ItemConfig itemConfig) {
            ItemConfig.Observe observe = itemConfig.observe;
            if (observe == null) return null;

            ActionCooldown leftClick = ActionCooldown.fromActions(observe.itemLeftClick);
            ActionCooldown rightClick = ActionCooldown.fromActions(observe.itemRightClick);

            if (leftClick == null && rightClick == null) return null;
            return new GearCooldown(leftClick, rightClick);
        }
    }

    public static class ActionCooldown {
        public final int durationTicks;
        @Nullable public final String failMessage;

        public int tickStart = Integer.MIN_VALUE;

        private ActionCooldown(int durationTicks, @Nullable String failMessage) {
            this.durationTicks = durationTicks;
            this.failMessage = failMessage;
        }

        public void start() { if (!isActive()) tickStart = cooldownTicks; }

        public void cancel() { tickStart = Integer.MIN_VALUE; }

        public boolean isActive() { return cooldownTicks < tickStart + durationTicks; }

        public float getPercent() {
            if (!isActive()) return 0f;
            int elapsed = cooldownTicks - tickStart;
            return 1f - (elapsed / (float) durationTicks);
        }

        @Nullable
        private static CooldownTracker.ActionCooldown fromActions(@Nullable List<ItemConfig.Action> actions) {
            if (actions == null) return null;

            String length = null;
            String failMessage = null;

            for (ItemConfig.Action action : actions) {
                if (action.cooldown != null) {
                    length = action.cooldown.length;
                } else if (action.ensure != null && action.ensure.onFail != null && !action.ensure.onFail.isEmpty()) {
                    ItemConfig.FailAction fail = action.ensure.onFail.getFirst();
                    if (fail.sendActionBar != null && fail.sendActionBar.text != null) {
                        failMessage = fail.sendActionBar.text.replaceAll("<[^>]*>", ""); // Strip all tags from fail message
                    }
                }
            }

            return length != null ? new ActionCooldown(parseLength(length), failMessage) : null;
        }

        public static int parseLength(String length) {
            int splitAt = 0;
            while (splitAt < length.length() && !Character.isLetter(length.charAt(splitAt))) {
                splitAt++;
            }

            if (splitAt == 0 || splitAt == length.length()) {
                throw new IllegalArgumentException("Not a valid duration: " + length);
            }

            double value = Double.parseDouble(length.substring(0, splitAt));
            String unit = length.substring(splitAt);

            final double SECOND = 20;
            final double MINUTE = SECOND * 60;
            final double HOUR = MINUTE * 60;
            final double DAY = HOUR * 24;
            final double WEEK = DAY * 7;
            final double MONTH = DAY * 31;

            return (int) switch (unit) {
                case "ms" -> value / 50.0;
                case "t"  -> value;
                case "s"  -> value * SECOND;
                case "m"  -> value * MINUTE;
                case "h"  -> value * HOUR;
                case "d"  -> value * DAY;
                case "w"  -> value * WEEK;
                case "mo" -> value * MONTH;
                default   -> throw new IllegalArgumentException("Unknown duration unit '" + unit + "' in: " + length);
            };
        }
    }

    public static void tick() {
        cooldownTicks++;
    }

    public static void onItemLeftClick(ItemStack item) { onItemClick(item, true); }

    public static void onItemRightClick(ItemStack item) { onItemClick(item, false); }

    private static void onItemClick(ItemStack item, boolean isLeftClick) {
        Identifier modelId = InventoryTracker.getMiAModelId(item);
        if (modelId == null) return;

        GearCooldown gearCooldown = itemCooldownConfigs.get(modelId);
        if (gearCooldown == null) return;

        ActionCooldown action = isLeftClick ? gearCooldown.leftClick : gearCooldown.rightClick;
        if (action != null) action.start();
    }

    public static boolean allowActionBarMessage(Component message) {
        if (!MiACompat.config.hideActionBarGearCooldowns) return true;

        String msg = message.getString();

        if (msg.contains("■■■■■")) return false; // Cooldown bar characters

        for (GearCooldown gearCooldown : itemCooldownConfigs.values()) {
            for (ActionCooldown action : new ActionCooldown[]{gearCooldown.leftClick, gearCooldown.rightClick}) {
                if (action != null && action.isActive() && msg.equals(action.failMessage)) {
                    action.cancel();
                    return false;
                }
            }
        }

        return true;
    }

    @Nullable
    public static GearCooldown getGearCooldown(Identifier modelId) { return itemCooldownConfigs.get(modelId); }

    public static void loadItemCooldownConfigs() {
        String itemConfigResource = "/assets/" + MiACompat.MOD_ID + "/config/items";

        Yaml yaml = new Yaml(ItemConfig.constructor(new LoaderOptions()));

        itemCooldownConfigs.clear();

        try {
            URL resourceUrl = MiACompat.class.getResource(itemConfigResource);
            if (resourceUrl == null) {
                MiACompat.LOGGER.error("Config resource not found: {}", itemConfigResource);
                return;
            }

            URI uri = resourceUrl.toURI();
            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    loadItemConfigsFromPath(fs.getPath(itemConfigResource), yaml);
                }
            } else {
                loadItemConfigsFromPath(Path.of(uri), yaml);
            }

        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to load item configs", e);
        }
    }

    private static void loadItemConfigsFromPath(Path rootPath, Yaml yaml) throws IOException {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(p -> p.toString().endsWith(".yml"))
                .forEach(p -> {
                    try (InputStream is = Files.newInputStream(p)) {
                        ItemConfig itemConfig = yaml.load(is);

                        Identifier itemModel = Identifier.tryParse(itemConfig.getItem().itemModel);
                        if (itemModel == null) {
                            MiACompat.LOGGER.warn("No itemModel found in: {}", p);
                            return;
                        }

                        GearCooldown cooldown = GearCooldown.fromItemConfig(itemConfig);
                        if (cooldown != null) {
                            itemCooldownConfigs.put(itemModel, cooldown);
                        } else {
                            MiACompat.LOGGER.warn("Item '{}' does not have a valid cooldown", itemModel);
                        }
                    } catch (IOException e) {
                        MiACompat.LOGGER.error("Failed to load config: {}", p, e);
                    }
                });
        }
    }
}
