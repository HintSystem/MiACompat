package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.schema.ItemConfig;

import net.minecraft.client.Minecraft;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

public class CooldownTracker {
    private static final Map<Identifier, GearCooldowns> gearCooldownsByItemModel = new HashMap<>();
    private static final Map<String, List<ActionCooldown>> actionCooldownByDisplay = new HashMap<>();
    private static final Set<String> actionFailMessages = new HashSet<>();

    private static final long MAX_COOLDOWN_TIME_DIFF_MS = 900;

    public static class GearCooldowns {
        @Nullable public final ActionCooldown leftClick;
        @Nullable public final ActionCooldown rightClick;

        public GearCooldowns(@Nullable ActionCooldown leftClick, @Nullable ActionCooldown rightClick) {
            this.leftClick = leftClick;
            this.rightClick = rightClick;
        }

        @Nullable
        public static GearCooldowns fromItemConfig(ItemConfig itemConfig) {
            ItemConfig.Observe observe = itemConfig.observe;
            if (observe == null) return null;

            ActionCooldown leftClick = ActionCooldown.fromActionsConfig(observe.itemLeftClick);
            ActionCooldown rightClick = ActionCooldown.fromActionsConfig(observe.itemRightClick);

            if (leftClick == null && rightClick == null) return null;
            return new GearCooldowns(leftClick, rightClick);
        }
    }

    public static class ActionCooldown {
        private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");

        public final long durationMs;
        @Nullable public final String cooldownDisplay;
        @Nullable public final String failMessage;

        private long triggeredTime = Long.MIN_VALUE;
        private long endTime = Long.MIN_VALUE;

        private ActionCooldown(long durationMs, @Nullable String cooldownDisplay, @Nullable String failMessage) {
            this.durationMs = durationMs;
            this.cooldownDisplay = cooldownDisplay;
            this.failMessage = failMessage;
        }

        private static String stripTags(String str) {
            if (str == null) return null;

            return TAG_PATTERN.matcher(str).replaceAll("");
        }

        @Nullable
        private static CooldownTracker.ActionCooldown fromActionsConfig(@Nullable List<ItemConfig.Action> actions) {
            if (actions == null) return null;

            String length = null;
            String cooldownDisplay = null;
            String failMessage = null;

            for (ItemConfig.Action action : actions) {
                if (action.cooldown != null) {
                    length = action.cooldown.length;
                    cooldownDisplay = stripTags(action.cooldown.display);
                } else if (action.ensure != null && action.ensure.onFail != null && !action.ensure.onFail.isEmpty()) {
                    ItemConfig.FailAction fail = action.ensure.onFail.getFirst();
                    if (fail.sendActionBar != null) {
                        failMessage = stripTags(fail.sendActionBar.text);
                    }
                }
            }

            return length != null ? new ActionCooldown(parseLength(length), cooldownDisplay, failMessage) : null;
        }

        /** Measured in milliseconds */
        public long getTriggeredTime() { return triggeredTime; }

        /** Measured in milliseconds */
        public long getTriggeredEndTime() { return getTriggeredTime() + durationMs; }

        /** Measured in milliseconds */
        public long getEndTime() { return endTime; }

        /** Measured in milliseconds */
        public long getRemainingTime() { return Math.max(0, getEndTime() - System.currentTimeMillis()); }

        public float getPercent() {
            if (!isActive())
                return 0f;

            return Math.clamp(
                getRemainingTime() / (float) durationMs,
                0f, 1f
            );
        }

        public boolean isActive() { return System.currentTimeMillis() < endTime; }

        /** Called when the player triggers a cooldown via input */
        public void trigger() { triggeredTime = System.currentTimeMillis(); }

        /**
         * Called after the cooldown has been confirmed to be correct
         * @return True, if cooldown was started
         */
        public boolean begin() {
            long expectedEnd = triggeredTime + durationMs;

            // begin() has already been called for the current trigger
            if (Math.abs(endTime - expectedEnd) < MAX_COOLDOWN_TIME_DIFF_MS)
                return false;

            endTime = expectedEnd;
            return true;
        }

        public void cancel() { endTime = System.currentTimeMillis(); }

        /** @return Length in milliseconds */
        public static long parseLength(String length) {
            int splitAt = 0;
            while (splitAt < length.length() && !Character.isLetter(length.charAt(splitAt))) {
                splitAt++;
            }

            if (splitAt == 0 || splitAt == length.length()) {
                throw new IllegalArgumentException("Not a valid duration: " + length);
            }

            double value = Double.parseDouble(length.substring(0, splitAt));
            String unit = length.substring(splitAt);

            final double TICK = 50;
            final double SECOND = 1_000;
            final double MINUTE = SECOND * 60;
            final double HOUR = MINUTE * 60;
            final double DAY = HOUR * 24;
            final double WEEK = DAY * 7;
            final double MONTH = DAY * 31;

            return (long) switch (unit) {
                case "ms" -> value;
                case "t" -> value * TICK;
                case "s" -> value * SECOND;
                case "m" -> value * MINUTE;
                case "h" -> value * HOUR;
                case "d" -> value * DAY;
                case "w" -> value * WEEK;
                case "mo" -> value * MONTH;
                default -> throw new IllegalArgumentException("Unknown duration unit '" + unit + "' in: " + length);
            };
        }
    }

    public static void onItemLeftClick(ItemStack item) { onItemClick(item, true); }

    public static void onItemRightClick(ItemStack item) { onItemClick(item, false); }

    public static void onItemClick(ItemStack item, boolean isLeftClick) {
        GearCooldowns gearCooldowns = getGearCooldowns(item);
        if (gearCooldowns == null) return;

        ActionCooldown action = isLeftClick ? gearCooldowns.leftClick : gearCooldowns.rightClick;
        if (action != null) action.trigger();
    }

    public static boolean allowActionBarMessage(Component message) {
        String msg = message.getString();

        if (MiACompat.config.hideAbilityFailsInActionBar && actionFailMessages.contains(msg))
            return false;

        if (isCooldownMessage(msg)) {
            Minecraft.getInstance().execute(() -> onCooldownMessage(msg));
            return !MiACompat.config.hideGearCooldownsInActionBar;
        }

        return true;
    }

    public static boolean isCooldownMessage(String message) {
        return message.contains("■■■■■");
    }

    public static void onCooldownMessage(String message) {
        String[] cooldowns = message.split(", ");
        long now = System.currentTimeMillis();

        for (String c : cooldowns) {
            int bar = c.indexOf('■');
            if (bar == -1) continue;

            String display = c.substring(0, bar).trim();
            List<ActionCooldown> actions = actionCooldownByDisplay.get(display);
            if (actions == null || actions.isEmpty()) {
                MiACompat.LOGGER.warn("Observed cooldown for '{}', but no actions were found", display);
                continue;
            }

            int open = c.indexOf('[', c.lastIndexOf('■'));
            int close = c.indexOf(']', open);

            if (open == -1 || close == -1) continue;

            String remainingText = c.substring(open + 1, close).trim();
            if (remainingText.endsWith("s")) {
                remainingText = remainingText.substring(0, remainingText.length() - 1);
            }

            try {
                long remainingMs = (long) (Double.parseDouble(remainingText) * 1000);
                long observedEndTime = now + remainingMs;
                ActionCooldown foundCooldown = findTriggeredCooldown(actions, observedEndTime);

                if (foundCooldown != null && foundCooldown.begin()) {
                    MiACompat.LOGGER.info("Began '{}' cooldown ({}ms diff)",
                        display, Math.abs(foundCooldown.getTriggeredEndTime() - observedEndTime));
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    @Nullable
    private static ActionCooldown findTriggeredCooldown(List<ActionCooldown> potentialCooldowns, long observedEndTime) {
        ActionCooldown closest = null;
        long closestDiff = Long.MAX_VALUE;

        for (ActionCooldown cooldown : potentialCooldowns) {
            long diff = Math.abs(cooldown.getTriggeredEndTime() - observedEndTime);

            if (diff < closestDiff) {
                closestDiff = diff;
                closest = cooldown;
            }
        }

        if (closestDiff > MAX_COOLDOWN_TIME_DIFF_MS) return null;
        return closest;
    }

    @Nullable
    public static GearCooldowns getGearCooldowns(ItemStack item) {
        Identifier modelId = InventoryTracker.getMiAModelId(item);
        if (modelId == null) return null;

        return getGearCooldowns(modelId);
    }

    @Nullable
    public static GearCooldowns getGearCooldowns(Identifier modelId) { return gearCooldownsByItemModel.get(modelId); }

    public static void loadItemCooldownConfigs() {
        String itemConfigResource = "/assets/" + MiACompat.MOD_ID + "/config/items";

        Yaml yaml = new Yaml(ItemConfig.constructor(new LoaderOptions()));

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
        gearCooldownsByItemModel.clear();
        actionCooldownByDisplay.clear();
        actionFailMessages.clear();

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

                        GearCooldowns cooldowns = GearCooldowns.fromItemConfig(itemConfig);
                        if (cooldowns == null) {
                            MiACompat.LOGGER.warn("Item '{}' does not have a valid cooldown", itemModel);
                            return;
                        }

                        registerGearCooldowns(itemModel, cooldowns);
                    } catch (IOException e) {
                        MiACompat.LOGGER.error("Failed to load config: {}\n\n{}", p, e);
                    }
                });
        }
    }

    private static void registerGearCooldowns(Identifier itemModel, GearCooldowns cooldowns) {
        gearCooldownsByItemModel.put(itemModel, cooldowns);

        registerActionCooldown(cooldowns.leftClick);
        registerActionCooldown(cooldowns.rightClick);
    }

    private static void registerActionCooldown(ActionCooldown cooldown) {
        if (cooldown == null) return;

        if (cooldown.cooldownDisplay != null) {
            actionCooldownByDisplay
                .computeIfAbsent(cooldown.cooldownDisplay, (k) -> new ArrayList<>())
                .add(cooldown);
        }

        if (cooldown.failMessage != null) {
            actionFailMessages.add(cooldown.failMessage);
        }
    }
}
