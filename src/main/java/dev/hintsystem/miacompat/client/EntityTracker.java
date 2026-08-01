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
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import org.jetbrains.annotations.Nullable;

public class EntityTracker {
    public static final int SCAN_ENTITIES_TICKS = 15;

    private static final Map<UUID, ScannedEntity> scannedEntities = new HashMap<>();

    private static int ticksScanned = 0;

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;

        ProfilerFiller profiler = Profiler.get();
        profiler.push("scanAddedEntities");

        if (ticksScanned++ >= SCAN_ENTITIES_TICKS) {
            ticksScanned = 0;
            scanAddedEntities(client.level);
        }

        profiler.popPush("updateScannedEntities");

        updateScannedEntities(client.level, client.player);

        profiler.pop();
    }

    public static void onEntityAttacked(Player player, Level level, Entity entity) {
        if (!(entity instanceof Interaction interaction)) return;

        List<Entity> entitiesNear = level.getEntities(player,
            interaction.getBoundingBox().inflate(1.5));

        Set<UUID> visited = new HashSet<>();
        for (Entity near : entitiesNear) {
            if (!(near instanceof Display.ItemDisplay itemDisplay)) continue;

            AttackedEntity attackedEntity = AttackedEntity.fromItemDisplay(itemDisplay);
            if (attackedEntity == null) continue;

            if (!visited.add(attackedEntity.uuid)) continue;

            // Prioritize attacked entities
            scannedEntities.compute(attackedEntity.uuid, (k, v) -> {
                if (!(v instanceof AttackedEntity existing)) {
                    attackedEntity.onAttacked();
                    return attackedEntity;
                }

                existing.onAttacked();
                return v;
            });
        }
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
        scannedEntities.values()
            .removeIf(scannedEntity -> scannedEntity.tickRemoval(level, player));
    }

    private static final class PrayingSkeleton extends AttackedEntity {
        public PrayingSkeleton(Entity entity, Identifier mobPartModelId) {
            super(entity, mobPartModelId, "praying_skeleton");
            this.removalAfterAttackTicks = 20;
        }

        @Override
        public void onAttacked() {
            super.onAttacked();

            if (!MiACompat.config.clearBreadcrumbsOnFind) return;
            MiACompat.ghostSeekTracker.clearMeasurements();
        }

        @Override
        protected void onKilled(Player player) {
            CompendiumTracker.addKilledPrayingSkeleton();

            MiACompat.LOGGER.info("Killed praying skeleton from {} blocks away ({})",
                Math.round(distanceTo(player)), mobPartModelId);
        }
    }

    public static class AttackedEntity extends ScannedEntity {
        protected int removalAfterAttackTicks = 80;

        public AttackedEntity(Entity entity, Identifier mobPartModelId, String mobName) {
            super(entity, mobPartModelId, mobName);
        }

        @Override
        public boolean tickRemoval(Level level, Player player) {
            if (ticksRemoved++ >= removalAfterAttackTicks) return true;

            if (!updateEntity(level) && !isOccluded(level, player)) {
                onKilled(player);
                return true;
            }

            return false;
        }

        public void onAttacked() {
            this.ticksRemoved = 0;
        }

        @Nullable
        public static AttackedEntity fromItemDisplay(Display.ItemDisplay itemDisplay) {
            Entity vehicle = itemDisplay.getVehicle();
            if (vehicle == null) return null;

            Identifier itemModel = itemDisplay.getItemStack().get(DataComponents.ITEM_MODEL);
            if (itemModel == null) return null;

            if (ServerMobRegistry.isPrayingSkeletonModel(itemModel))
                return new PrayingSkeleton(vehicle, itemModel);

            return null;
        }
    }

    public static class ScannedEntity {
        public static final int MODEL_VIEW_DISTANCE = 48;
        public static final int ENTITY_VIEW_DISTANCE_SQR = (MODEL_VIEW_DISTANCE-1) * (MODEL_VIEW_DISTANCE-1);
        public static final int ENTITY_VERT_VIEW_DISTANCE = 30;

        public static final int MAX_REMOVAL_TICKS = 40;

        public final UUID uuid;
        protected final Identifier mobPartModelId;
        protected final String mobName;

        protected int ticksRemoved = 0;
        protected Vec3 lastPosition;

        public ScannedEntity(
            Entity entity,
            Identifier mobPartModelId, String mobName
        ) {
            this.uuid = entity.getUUID();
            this.mobPartModelId = mobPartModelId;
            this.mobName = mobName;

            updateEntity(entity.level());
        }

        public int getTicksRemoved() { return this.ticksRemoved; }

        public Vec3 getLastPosition() { return this.lastPosition; }

        public double distanceTo(Entity entity) { return this.lastPosition.distanceTo(entity.position()); }

        public boolean isOccluded(Level level, Player player) {
            if (Math.abs(this.lastPosition.y - player.position().y) >= ENTITY_VERT_VIEW_DISTANCE)
                return true;

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

        /** @return {@code true}, if entity still exists */
        public boolean updateEntity(Level level) {
            Entity entity = level.getEntity(uuid);
            if (entity == null) return false;

            this.lastPosition = entity.position();
            return true;
        }

        /** @return {@code true}, if entity should be removed this tick */
        public boolean tickRemoval(Level level, Player player) {
            // entity still exists
            if (updateEntity(level)) {
                this.ticksRemoved = 0;
                return false;
            }

            boolean instantRemoval = (this.ticksRemoved++ == 0)
                && isOccluded(level, player);

            // entity disappeared while it was blocked from view, remove it
            if (instantRemoval) return true;

            // entity was removed while in view and didn't return, consider killed
            if (ticksRemoved > MAX_REMOVAL_TICKS) {
                onKilled(player);
                return true;
            }

            return false;
        }

        protected void onKilled(Player player) {
            CompendiumTracker.addKilledMob(mobName);

            MiACompat.LOGGER.info("Killed mob '{}' from {}({}) blocks away ({})",
                mobName, Math.round(distanceTo(player)), lastPosition.y - player.position().y, mobPartModelId);
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
