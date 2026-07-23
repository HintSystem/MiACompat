package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.config.PersistentGsonData;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.AABB;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public class BonfireTracker {
    public static Display.ItemDisplay trackedBonfireEntity;
    public static final BonfireData bonfireData = new BonfireData();

    private static final int MAX_LOST_BONFIRE_TICKS = 10;
    private static int lostBonfireTicks = 0;

    public static void tick(Minecraft client) {
        if (!MiACompat.isMiAServer()) return;

        if (updateTrackedBonfireState()) return; // Skip if the bonfire entity is loaded

        if (client.level == null || client.player == null) return;

        int blockViewDistance = client.options.getEffectiveRenderDistance() * 16;
        double bonfireSquaredDistance = bonfireData.getBlockPos().distToCenterSqr(client.player.position());

        // Check if bonfire is close enough to be loaded
        if (bonfireSquaredDistance < blockViewDistance * blockViewDistance) {
            Display.ItemDisplay bonfire = findBonfire(
                client.level.getEntities(client.player, new AABB(bonfireData.getBlockPos()))
            );

            if (bonfire != null) {
                setTrackedBonfire(bonfire);
                return;
            }

            // Unlink bonfire if it can't be found within tick limit
            if (++lostBonfireTicks >= MAX_LOST_BONFIRE_TICKS) {
                updateBonfireStatus(false);
            }
            return;
        }

        lostBonfireTicks = 0;
    }

    public static void onServerMessage(Component message) {
        if (trackedBonfireEntity != null) return;

        String msg = message.getString().toLowerCase(Locale.ROOT);

        if (msg.contains("respawn point has been removed")
            || msg.contains("respawn point was unset")) {
            updateBonfireStatus(false);
        }
    }

    public static void setTrackedBonfire(Display.ItemDisplay bonfireEntity) {
        lostBonfireTicks = 0;
        trackedBonfireEntity = bonfireEntity;
        updateTrackedBonfireState();
    }

    public static boolean updateTrackedBonfireState() {
        if (trackedBonfireEntity == null) return false;
        if (trackedBonfireEntity.isRemoved()) {
            trackedBonfireEntity = null;
            return false;
        }

        CustomModelData modelData = trackedBonfireEntity.getItemStack().get(DataComponents.CUSTOM_MODEL_DATA);
        boolean isBonfireSet = modelData != null && modelData.flags().size() >= 2 && modelData.flags().get(1);
        bonfireData.setPos(trackedBonfireEntity.blockPosition());

        updateBonfireStatus(isBonfireSet);
        return true;
    }

    public static void updateBonfireStatus(boolean isBonfireSet) {
        if (bonfireData.isBonfireSet != isBonfireSet) {
            if (isBonfireSet) bonfireData.lastSetTimestamp = Util.getEpochMillis();
            bonfireData.isBonfireSet = isBonfireSet;
            bonfireData.saveToFile();

            MiACompat.LOGGER.info("[MiACompat] Bonfire {} detected! ({})",
                isBonfireSet ? "spawn point set" : "spawn point remove",
                trackedBonfireEntity != null ? trackedBonfireEntity.getItemStack().get(DataComponents.CUSTOM_MODEL_DATA) : null);
        }
    }

    public static boolean isBonfireId(Identifier modelId) {
        return modelId.getNamespace().equals(MiACompat.getMiANamespace())
            && modelId.getPath().contains("bonfire");
    }

    @Nullable
    public static Display.ItemDisplay findBonfire(List<Entity> entityList) {
        for (Entity entity : entityList) {
            if (entity instanceof Display.ItemDisplay displayEntity) {
                ItemStack stack = displayEntity.getItemStack();
                Identifier itemModel = stack.get(DataComponents.ITEM_MODEL);

                if (itemModel != null && isBonfireId(itemModel)) return displayEntity;
            }
        }

        return null;
    }

    public static void loadFromFile() { bonfireData.loadFromFile(); }

    public static class BonfireData extends PersistentGsonData<BonfireData> {
        public int x, y, z;
        public boolean isBonfireSet;
        public long lastSetTimestamp;

        public BonfireData() {
            setPos(BlockPos.ZERO);
            this.isBonfireSet = false;
            this.lastSetTimestamp = 0;
        }

        public void setPos(Vec3i pos) { this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ(); }
        public BlockPos getBlockPos() { return new BlockPos(x, y, z); }

        @Override
        public String getDataTitle() { return "MiACompat bonfire data"; }

        @Override
        public Path getFilePath() { return MiACompat.CONFIG_FOLDER.resolve("bonfire.json"); }

        @Override
        protected void applyData(BonfireData data) {
            setPos(data.getBlockPos());
            this.isBonfireSet = data.isBonfireSet;
            this.lastSetTimestamp = data.lastSetTimestamp;
        }

        @Override
        protected Class<BonfireData> getDataClass() { return BonfireData.class; }
    }
}
