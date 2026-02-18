package dev.hintsystem.miacompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GhostSeekTracker {
    private static final double MIN_MEASUREMENT_DISTANCE = 6.0;

    private static final String GHOST_SEEK_ITEM_NAME = "ghost seek";
    private static final String PRAYING_SKELETON_PREFIX = "praying_skeleton";

    private static final int CACHE_DURATION = 20;
    private int cacheValidTicks = 0;
    public int awaitingPingTicks = 0;

    private GhostSeekType cachedGhostSeekType = null;
    private ItemStack cachedGhostSeek = null;

    private final List<Measurement> measurements = new ArrayList<>();

    public static class Measurement {
        public final Instant timestamp;
        public final Vec3 position;
        public final double distance;
        public final double uncertainty;
        public final Integer pingLength;

        public Measurement(Vec3 position, double distance, double uncertainty, Integer pingLength) {
            this.timestamp = Instant.now();
            this.position = position;
            this.distance = distance;
            this.uncertainty = uncertainty;
            this.pingLength = pingLength;
        }

        public int getColor(int maxRange) {
            List<Color> measurementColors = MiACompat.config.breadcrumbColors;

            if (pingLength != null) {
                int index = Math.clamp(pingLength - 1, 0, measurementColors.size() - 1);
                return measurementColors.get(index).getRGB();
            }

            double effectiveDistance = Math.max(distance - uncertainty, 0.0);
            float t = 1.0f - (float)(effectiveDistance / maxRange);
            t = Math.clamp(t, 0.0f, 1.0f);

            return lerpColorRamp(measurementColors, t);
        }

        private static int lerpColorRamp(List<Color> colors, float t) {
            if (colors.isEmpty()) return 0;
            if (colors.size() == 1) return colors.getFirst().getRGB();

            float scaled = t * (colors.size() - 1);
            int i0 = (int) Math.floor(scaled);
            int i1 = Math.min(i0 + 1, colors.size() - 1);
            float localT = scaled - i0;

            return ARGB.linearLerp(
                localT,
                colors.get(i0).getRGB(),
                colors.get(i1).getRGB()
            );
        }
    }

    public void tick(Minecraft client) {
        if (MiACompat.config.breadcrumbDuration > 0) {
            measurements.removeIf(m -> Instant.now().isAfter(
                m.timestamp.plusSeconds(MiACompat.config.breadcrumbDuration)
            ));
        }

        if (awaitingPingTicks > 0) awaitingPingTicks--;
        if (cacheValidTicks > 0) cacheValidTicks--;
    }

    @Nullable
    public Component onActionbarMessage(Component message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;

        String messageText = message.getString().trim().toLowerCase();
        int pingLength = parsePingLength(messageText);

        if (pingLength == 0) return null;

        GhostSeekType type = getGhostSeekType();
        if (type == null) return null;

        awaitingPingTicks = type.pingIntervalTicks;
        InclusiveRange<@NotNull Integer> pingRange = type.getPingRange(pingLength);
        Measurement measurement = type.getPingMeasurement(player.position(), pingLength);
        addMeasurement(measurement);

        String range = "%d-%d blocks".formatted(pingRange.minInclusive(), pingRange.maxInclusive());
        MiACompat.LOGGER.info("Ghost seek ping: {}, range: {}", pingLength, range);

        if (!MiACompat.config.ghostSeekDistanceHint && !MiACompat.config.pingColorMatchesBreadcrumb) return null;

        MutableComponent editedMessage = message.copy();
        if (MiACompat.config.ghostSeekDistanceHint) {
            editedMessage.append(" (" + range + ")");
        }

        if (MiACompat.config.pingColorMatchesBreadcrumb) {
            editedMessage.setStyle(Style.EMPTY.withColor(measurement.getColor(type.getMaxRange())));
        }

        return editedMessage;
    }

    public List<Measurement> getMeasurements() { return new ArrayList<>(measurements); }

    public void addMeasurement(Measurement measurement) {
        if (MiACompat.config.breadcrumbDuration <= 0) return;

        for (Measurement existing : measurements) {
            if (existing.position.distanceTo(measurement.position) < MIN_MEASUREMENT_DISTANCE) return;
        }

        measurements.add(measurement);
    }

    public void clearMeasurements() { measurements.clear(); }

    public boolean breadcrumbsVisible() {
        return MiACompat.config.breadcrumbDuration > 0
            && getGhostSeekType() != null;
    }

    public static boolean isPrayingSkeleton(Display.ItemDisplay itemDisplayEntity) {
        ItemStack stack = itemDisplayEntity.getItemStack();
        Identifier modelName = stack.get(DataComponents.ITEM_MODEL);

        return modelName != null && modelName.getPath().startsWith(PRAYING_SKELETON_PREFIX);
    }

    public int getMaxRange() {
        return (cachedGhostSeekType != null) ? cachedGhostSeekType.getMaxRange() : 150;
    }

    /** @return last used ghost seek type, null if it hasn't been equipped before */
    @Nullable
    public GhostSeekType getLastGhostSeekType() { return cachedGhostSeekType; }

    @Nullable
    public GhostSeekType getGhostSeekType() {
        boolean valid = updateGhostSeek();
        return valid ? cachedGhostSeekType : null;
    }

    @Nullable
    public ItemStack getGhostSeek() {
        boolean valid = updateGhostSeek();
        return valid ? cachedGhostSeek : null;
    }

    private static int parsePingLength(String message) {
        return switch (message) {
            case String s when s.startsWith("du du du du dum") -> 5;
            case String s when s.startsWith("du du du dum") -> 4;
            case String s when s.startsWith("du du dum") -> 3;
            case String s when s.startsWith("du dum") -> 2;
            case String s when s.startsWith("dum") -> 1;
            default -> 0;
        };
    }

    public enum GhostSeekType {
        MAKESHIFT("makeshift", 20, new int[] {150, 100, 50, 25}),
        REPAIRED("repaired", 20, new int[] {200, 150, 100, 50, 25}),
        REFINED("refined", 15, new int[] {250, 150, 100, 50, 25});

        private final String itemName;
        public final int pingIntervalTicks;
        private final int[] ranges;

        GhostSeekType(String itemName, int pingIntervalSec, int[] ranges) {
            this.itemName = itemName;
            this.pingIntervalTicks = pingIntervalSec * 20;
            this.ranges = ranges;
        }

        public int getMaxRange() { return ranges[0]; }

        public InclusiveRange<@NotNull Integer> getPingRange(int pingLength) {
            int rangeIndex = Math.clamp(pingLength - 1, 0, ranges.length - 1);
            int minDistance = (rangeIndex < ranges.length - 1) ? ranges[rangeIndex + 1] : 0;
            int maxDistance = ranges[rangeIndex];

            return new InclusiveRange<>(minDistance, maxDistance);
        }

        public Measurement getPingMeasurement(Vec3 pos, int pingLength) {
            InclusiveRange<@NotNull Integer> pingRange = getPingRange(pingLength);

            double midDistance = (pingRange.maxInclusive() + pingRange.minInclusive()) / 2.0;
            double uncertainty = (pingRange.maxInclusive() - pingRange.minInclusive()) / 2.0;

            return new Measurement(pos, midDistance, uncertainty, pingLength);
        }

        public static GhostSeekType fromItemStack(ItemStack stack) {
            Component itemName = stack.get(DataComponents.ITEM_NAME);
            if (itemName == null) return MAKESHIFT;

            String name = itemName.getString().toLowerCase();

            for (GhostSeekType type : GhostSeekType.values()) {
                if (name.contains(type.itemName)) {
                    return type;
                }
            }

            return MAKESHIFT;
        }
    }

    /** @return true, if the ghost seek cache is valid */
    private boolean updateGhostSeek() {
        // Use cache to avoid repeated inventory checks
        if (cacheValidTicks > 0) return cachedGhostSeek != null;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            cachedGhostSeek = null;
            return false;
        }

        Inventory inventory = player.getInventory();

        for (int slotIndex : InventoryTracker.PASSIVE_SLOTS) {
            ItemStack stack = inventory.getItem(slotIndex);
            if (isItemGhostSeek(stack)) {
                cachedGhostSeekType = GhostSeekType.fromItemStack(stack);
                cachedGhostSeek = stack;
                cacheValidTicks = CACHE_DURATION;
                return true;
            }
        }

        cachedGhostSeek = null;
        cacheValidTicks = CACHE_DURATION;
        return false;
    }

    private static boolean isItemGhostSeek(ItemStack stack) {
        Component itemName = stack.get(DataComponents.ITEM_NAME);
        if (itemName == null) return false;

        return itemName.getString().toLowerCase().contains(GHOST_SEEK_ITEM_NAME);
    }
}