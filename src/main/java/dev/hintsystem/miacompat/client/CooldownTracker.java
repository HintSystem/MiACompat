package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.ServerItemRegistry;
import dev.hintsystem.miacompat.server.config.geary.item.ActionCooldown;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CooldownTracker {
    public static final int MAX_LATENCY_COMPENSATION_MS = 1200;

    public static void onItemLeftClick(ItemStack item) { onItemClick(item, true); }

    public static void onItemRightClick(ItemStack item) { onItemClick(item, false); }

    public static void onItemClick(ItemStack item, boolean isLeftClick) {
        ItemConfig itemConfig = ServerItemRegistry.getItem(item);
        if (itemConfig == null || itemConfig.itemCooldowns == null) return;

        ActionCooldown action = isLeftClick ? itemConfig.itemCooldowns.leftClick : itemConfig.itemCooldowns.rightClick;
        if (action != null)
            action.trigger();
    }

    public static boolean allowActionBarMessage(Component message) {
        String msg = message.getString();

        if (MiACompat.config.hideAbilityFailsInActionBar && ServerItemRegistry.isActionFailMessage(msg))
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
        long now = System.currentTimeMillis();
        String[] cooldowns = message.split(", ");

        for (String c : cooldowns) {
            int bar = c.indexOf('■');
            if (bar == -1) continue;

            String display = c.substring(0, bar).trim();
            List<ActionCooldown> potentialCooldowns = ServerItemRegistry.getActionCooldownsByDisplay(display);
            if (potentialCooldowns.isEmpty()) {
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

                List<ActionCooldown> cooldownCandidates = ActionCooldown
                    .filterCooldownCandidates(potentialCooldowns, remainingMs);

                if (cooldownCandidates.size() == 1) {
                    ActionCooldown uniqueCooldown = cooldownCandidates.getFirst();

                    if (uniqueCooldown.beginFromServer(observedEndTime)) {
                        MiACompat.LOGGER.info("Began unique '{}' cooldown ({}ms diff, {}ms ping)", display,
                            uniqueCooldown.getTriggeredEndTime() - observedEndTime,
                            MiACompat.getServerLatency()
                        );
                    }

                    return;
                }

                ActionCooldown foundCooldown = ActionCooldown
                    .findTriggeredCooldown(cooldownCandidates, observedEndTime);

                if (foundCooldown != null && foundCooldown.beginFromServer(observedEndTime)) {
                    MiACompat.LOGGER.info("Began '{}' cooldown ({}ms diff, {}ms ping)", display,
                        foundCooldown.getTriggeredEndTime() - observedEndTime,
                        MiACompat.getServerLatency()
                    );
                }
            } catch (NumberFormatException ignored) {}
        }
    }
}
