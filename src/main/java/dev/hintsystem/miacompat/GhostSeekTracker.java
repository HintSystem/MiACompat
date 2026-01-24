package dev.hintsystem.miacompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GhostSeekTracker {
    private static final double MIN_MEASUREMENT_DISTANCE = 6.0;

    private static final int[] PASSIVE_SLOTS = {9, 10};
    private static final String GHOST_SEEK_ITEM_NAME = "ghost seek";

    private GhostSeekType cachedGhostSeekType = null;
    private ItemStack cachedGhostSeek = null;
    private int cacheValidTicks = 0;
    private static final int CACHE_DURATION = 20;

    private final List<Measurement> measurements = new ArrayList<>();

    public static class Measurement {
        public final Instant timestamp;
        public final Vec3 position;
        public final double distance;
        public final double uncertainty;

        public Measurement(Vec3 position, double distance, double uncertainty) {
            this.timestamp = Instant.now();
            this.position = position;
            this.distance = distance;
            this.uncertainty = uncertainty;
        }
    }

    public void tick(Minecraft client) {
        if (MiACompat.config.ghostSeekBreadcrumbDuration > 0) {
            measurements.removeIf(m -> Instant.now().isAfter(
                m.timestamp.plusSeconds(MiACompat.config.ghostSeekBreadcrumbDuration)
            ));
        }

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

        InclusiveRange<Integer> pingRange = type.getPingRange(pingLength);
        Measurement measurement = type.getPingMeasurement(player.position(), pingLength);
        addMeasurement(measurement);

        String range = "%d-%d blocks".formatted(pingRange.minInclusive(), pingRange.maxInclusive());
        MiACompat.LOGGER.info("Ghost seek ping: {}, range: {}", pingLength, range);

        if (!MiACompat.config.ghostSeekDistanceHint) return null;

        return message.copy()
            .append(Component.literal(" (" + range + ")")).setStyle(message.getStyle());
    }

    public List<Measurement> getMeasurements() { return new ArrayList<>(measurements); }

    public void addMeasurement(Measurement measurement) {
        if (MiACompat.config.ghostSeekBreadcrumbDuration <= 0) return;

        for (Measurement existing : measurements) {
            if (existing.position.distanceTo(measurement.position) < MIN_MEASUREMENT_DISTANCE) return;
        }

        measurements.add(measurement);
    }

    public void clearMeasurements() { measurements.clear(); }

    @Nullable
    public GhostSeekType getGhostSeekType() {
        updateGhostSeek();
        return cachedGhostSeekType;
    }

    @Nullable
    public ItemStack getGhostSeek() {
        updateGhostSeek();
        return cachedGhostSeek;
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
        MAKESHIFT("makeshift", new int[] {150, 100, 50, 25}),
        REPAIRED("repaired", new int[] {200, 150, 100, 50, 25}),
        REFINED("refined", new int[] {250, 150, 100, 50, 25});

        private final String itemName;
        private final int[] ranges;

        GhostSeekType(String itemName, int[] ranges) {
            this.itemName = itemName;
            this.ranges = ranges;
        }

        public int getMaxRange() { return ranges[0]; }

        public InclusiveRange<Integer> getPingRange(int pingLength) {
            int rangeIndex = Math.clamp(pingLength - 1, 0, ranges.length - 1);
            int minDistance = (rangeIndex < ranges.length - 1) ? ranges[rangeIndex + 1] : 0;
            int maxDistance = ranges[rangeIndex];

            return new InclusiveRange<>(minDistance, maxDistance);
        }

        public Measurement getPingMeasurement(Vec3 pos, int pingLength) {
            InclusiveRange<Integer> pingRange = getPingRange(pingLength);

            double midDistance = (pingRange.maxInclusive() + pingRange.minInclusive()) / 2.0;
            double uncertainty = (pingRange.maxInclusive() - pingRange.minInclusive()) / 2.0;

            return new Measurement(pos, midDistance, uncertainty);
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

    private void updateGhostSeek() {
        // Use cache to avoid repeated inventory checks
        if (cacheValidTicks > 0) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            cachedGhostSeekType = null;
            cachedGhostSeek = null;
            return;
        }

        Inventory inventory = player.getInventory();

        for (int slotIndex : PASSIVE_SLOTS) {
            ItemStack stack = inventory.getItem(slotIndex);
            if (isItemGhostSeek(stack)) {
                cachedGhostSeekType = GhostSeekType.fromItemStack(stack);
                cachedGhostSeek = stack;
                cacheValidTicks = CACHE_DURATION;
                return;
            }
        }

        cachedGhostSeekType = null;
        cachedGhostSeek = null;
        cacheValidTicks = CACHE_DURATION;
    }

    private static boolean isItemGhostSeek(ItemStack stack) {
        Component itemName = stack.get(DataComponents.ITEM_NAME);
        if (itemName == null) return false;

        return itemName.getString().toLowerCase().contains(GHOST_SEEK_ITEM_NAME);
    }
}