package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.config.PersistentGsonData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.nio.file.Path;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public class BonfireTracker {
    public static final int MAX_LOST_BONFIRE_TICKS = 8;

    private static Display.ItemDisplay trackedBonfireEntity;
    private static boolean pendingBonfireLink = false;
    private static int lostBonfireTicks = 0;

    public static final BonfireData bonfireData = new BonfireData();

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (!MiACompat.isMiAServer()) return;

        // Skip if the bonfire entity is loaded
        if (updateTrackedBonfireState())
            return;

        if (isBonfireInLoadRange(client, client.player)) {
            Display.ItemDisplay bonfire = findBonfire(
                client.level.getEntities(client.player, new AABB(bonfireData.getBlockPos()))
            );

            if (bonfire != null) {
                setTrackedBonfire(bonfire);
                return;
            }

            // Unlink bonfire if it can't be found within tick limit
            if (++lostBonfireTicks == MAX_LOST_BONFIRE_TICKS) {
                setBonfireLinked(false);
            }
            return;
        }

        lostBonfireTicks = 0;
    }

    private static boolean isBonfireInLoadRange(Minecraft client, LocalPlayer player) {
        int viewDistance = client.options.getEffectiveRenderDistance() * 16;
        double distance = bonfireData.getBlockPos()
            .distToCenterSqr(player.position());

        return distance < viewDistance * viewDistance;
    }

    public static void onInteraction(Level level, Player player, Interaction interaction) {
        Display.ItemDisplay bonfire = BonfireTracker.findBonfire(
            level.getEntities(player, interaction.getBoundingBox().inflate(0.5))
        );

        if (bonfire == null) return;

        pendingBonfireLink = true;
        BonfireTracker.setTrackedBonfire(bonfire);
    }

    public static void onServerMessage(Component message) {
        if (trackedBonfireEntity != null) return;

        String msg = message.getString().toLowerCase(Locale.ROOT);

        if (msg.contains("respawn point has been removed")
            || msg.contains("respawn point was unset")
            || msg.contains("bonfire was not found")) {
            setBonfireLinked(false);
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
        bonfireData.setPos(trackedBonfireEntity.blockPosition());

        boolean isBonfireLinked = modelData != null
            && modelData.flags().size() >= 2
            && modelData.flags().get(1);

        if (isBonfireLinked && pendingBonfireLink) {
            bonfireData.lastLinkedTimestamp = Util.getEpochMillis();
            pendingBonfireLink = false;
        }

        setBonfireLinked(isBonfireLinked);
        return true;
    }

    public static void setBonfireLinked(boolean isBonfireLinked) {
        if (bonfireData.isLinked == isBonfireLinked)
            return;

        bonfireData.isLinked = isBonfireLinked;
        bonfireData.saveToFile();

        MiACompat.LOGGER.info("Bonfire {}! ({})",
            isBonfireLinked ? "spawn point set" : "spawn point removed",
            trackedBonfireEntity != null ? trackedBonfireEntity.getItemStack().get(DataComponents.CUSTOM_MODEL_DATA) : null);
    }

    public static boolean isBonfireId(Identifier modelId) {
        return modelId.getNamespace().equals(MiACompat.getMiANamespace())
            && modelId.getPath().contains("bonfire");
    }

    @Nullable
    public static Display.ItemDisplay findBonfire(Iterable<Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof Display.ItemDisplay displayEntity) {
                ItemStack stack = displayEntity.getItemStack();
                Identifier itemModel = stack.get(DataComponents.ITEM_MODEL);

                if (itemModel != null && isBonfireId(itemModel))
                    return displayEntity;
            }
        }

        return null;
    }

    public static void loadFromFile() { bonfireData.loadFromFile(); }

    public static class BonfireData extends PersistentGsonData<BonfireData> {
        public int x, y, z;
        public boolean isLinked;
        public long lastLinkedTimestamp;

        public BonfireData() {
            setPos(BlockPos.ZERO);
            this.isLinked = false;
            this.lastLinkedTimestamp = 0;
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
            this.isLinked = data.isLinked;
            this.lastLinkedTimestamp = data.lastLinkedTimestamp;
        }

        @Override
        protected Class<BonfireData> getDataClass() { return BonfireData.class; }
    }
}
