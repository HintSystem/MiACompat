package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.ServerMobRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class EntityTracker {
    public static final int SCAN_ENTITIES_TICKS = 15;
    public static final int MAX_REMOVAL_TICKS = 40;

    private static final Map<UUID, ScannedEntity> scannedEntities = new HashMap<>();

    private static int ticksScanned = 0;

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;

        ProfilerFiller profiler = Profiler.get();
        profiler.push("scanAddedEntities");

        ticksScanned = (ticksScanned + 1) % SCAN_ENTITIES_TICKS;
        if (ticksScanned == 0)
            scanAddedEntities(client.level);

        profiler.popPush("updateScannedEntities");

        updateScannedEntities(client.level, client.player);

        profiler.pop();
    }

    public static void scanAddedEntities(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Display.ItemDisplay itemDisplay)) continue;

            ScannedEntity scannedEntity = ScannedEntity.fromItemDisplay(itemDisplay);
            if (scannedEntity == null) continue;

            scannedEntities.putIfAbsent(scannedEntity.uuid, scannedEntity);
        }
    }

    private static void updateScannedEntities(ClientLevel level, LocalPlayer player) {
        Iterator<ScannedEntity> it = scannedEntities.values().iterator();

        while (it.hasNext()) {
            ScannedEntity scannedEntity = it.next();

            if (scannedEntity.updateEntity(level))
                continue;

            if (scannedEntity.tickRemoval() && scannedEntity.isOccluded(level, player)) {
                it.remove();
                continue;
            }

            if (scannedEntity.getTicksRemoved() > MAX_REMOVAL_TICKS) {
                it.remove();

                CompendiumTracker.addKilledMob(scannedEntity.mobModelId);

                MiACompat.LOGGER.info("Killed mob '{}' from {} blocks away ({})",
                    scannedEntity.mobModelId, Math.round(scannedEntity.distanceTo(player)),
                    scannedEntity.mobPartModelId);
            }
        }
    }

    public static class ScannedEntity {
        public static final int MODEL_VIEW_DISTANCE = 48;
        public static final int ENTITY_VIEW_DISTANCE_SQR = (MODEL_VIEW_DISTANCE-1) * (MODEL_VIEW_DISTANCE-1);

        public final UUID uuid;
        private final Identifier mobPartModelId;
        private final String mobModelId;

        private int ticksRemoved = 0;
        private Vec3 lastPosition;

        public ScannedEntity(
            Entity entity,
            Identifier mobPartModelId, String mobModelId
        ) {
            this.uuid = entity.getUUID();
            this.mobPartModelId = mobPartModelId;
            this.mobModelId = mobModelId;

            updateEntity(entity.level());
        }

        public int getTicksRemoved() { return this.ticksRemoved; }

        public Vec3 getLastPosition() { return this.lastPosition; }

        public double distanceTo(Entity entity) { return this.lastPosition.distanceTo(entity.position()); }

        public boolean isOccluded(Level level, Player player) {
            if (player.distanceToSqr(this.lastPosition) >= ENTITY_VIEW_DISTANCE_SQR)
                return true;

            BlockHitResult result = level.clip(new ClipContext(
                player.getEyePosition(), this.lastPosition.add(0, 0.4, 0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
            ));

            return result.getType() != HitResult.Type.MISS;
        }

        /** @return {@code false}, if entity no longer exists */
        public boolean updateEntity(Level level) {
            Entity entity = level.getEntity(uuid);
            if (entity == null) return false;

            this.lastPosition = entity.position();
            this.ticksRemoved = 0;
            return true;
        }

        /** @return {@code true}, if entity is removed for first tick */
        public boolean tickRemoval() {
            return this.ticksRemoved++ == 0;
        }

        @Nullable
        public static ScannedEntity fromItemDisplay(Display.ItemDisplay itemDisplay) {
            Entity vehicle = itemDisplay.getVehicle();
            if (vehicle == null) return null;

            Identifier itemModel = itemDisplay.getItemStack().get(DataComponents.ITEM_MODEL);
            if (itemModel == null) return null;

            String mobModelId = ServerMobRegistry.getMobModelId(itemModel);
            if (mobModelId == null) return null;

            return new ScannedEntity(vehicle, itemModel, mobModelId);
        }
    }
}
