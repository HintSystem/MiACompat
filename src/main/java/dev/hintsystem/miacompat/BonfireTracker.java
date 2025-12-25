package dev.hintsystem.miacompat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BonfireTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH = MiACompat.CONFIG_DIR.resolve(MiACompat.MOD_ID + "-bonfire.json");

    public static DisplayEntity.ItemDisplayEntity trackedBonfireEntity;
    public static BonfireData bonfireData = new BonfireData();

    public static class BonfireData {
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
    }

    public static void tick(MinecraftClient client) {
        boolean isBonfireTracked = updateTrackedBonfireState();

        // Update untracked bonfire
        if (!isBonfireTracked && MiACompat.isMiAServer()) {
            if (client.world == null || client.player == null) return;

            int blockViewDistance = client.options.getClampedViewDistance() * 16;
            double bonfireSquaredDistance = bonfireData.getBlockPos().getSquaredDistance(client.player.getPos());

            // Check if bonfire is close enough to be loaded
            if (bonfireSquaredDistance < blockViewDistance * blockViewDistance) {
                DisplayEntity.ItemDisplayEntity bonfire = findBonfire(
                    client.world.getOtherEntities(client.player, new Box(bonfireData.getBlockPos()))
                );

                if (bonfire != null) {
                    setTrackedBonfire(bonfire);
                } else {
                    setBonfireStatus(false);
                }
            }
        }
    }

    public static void onServerMessage(Text message) {
        if (trackedBonfireEntity != null) return;

        String msg = message.getString().toLowerCase();

        if (msg.contains("respawn point has been removed")
            || msg.contains("respawn point was unset")) {
            setBonfireStatus(false);
        }
    }

    public static void setTrackedBonfire(DisplayEntity.ItemDisplayEntity bonfireEntity) {
        trackedBonfireEntity = bonfireEntity;
        updateTrackedBonfireState();
    }

    public static boolean updateTrackedBonfireState() {
        if (trackedBonfireEntity == null) return false;
        if (trackedBonfireEntity.isRemoved()) {
            trackedBonfireEntity = null;
            return false;
        }

        CustomModelDataComponent modelData = trackedBonfireEntity.getItemStack().get(DataComponentTypes.CUSTOM_MODEL_DATA);
        boolean isBonfireSet = modelData != null && modelData.flags().size() >= 2 && modelData.flags().get(1);
        bonfireData.setPos(trackedBonfireEntity.getBlockPos());

        setBonfireStatus(isBonfireSet);
        return true;
    }

    public static void setBonfireStatus(boolean isBonfireSet) {
        if (bonfireData.isBonfireSet != isBonfireSet) {
            if (isBonfireSet) bonfireData.lastSetTimestamp = Util.getEpochTimeMs();
            bonfireData.isBonfireSet = isBonfireSet;
            saveToFile();

            MiACompat.LOGGER.info("[MiACompat] Bonfire {} detected! ({})",
                isBonfireSet ? "spawn point set" : "spawn point remove",
                trackedBonfireEntity != null ? trackedBonfireEntity.getItemStack().get(DataComponentTypes.CUSTOM_MODEL_DATA) : null);
        }
    }

    public static boolean isBonfireId(Identifier modelId) {
        return modelId.getNamespace().equals("mineinabyss")
            && modelId.getPath().contains("bonfire");
    }

    @Nullable
    public static DisplayEntity.ItemDisplayEntity findBonfire(List<Entity> entityList) {
        for (Entity entity : entityList) {
            if (entity instanceof DisplayEntity.ItemDisplayEntity displayEntity) {
                ItemStack stack = displayEntity.getItemStack();
                Identifier itemModel = stack.get(DataComponentTypes.ITEM_MODEL);

                if (itemModel != null && isBonfireId(itemModel)) return displayEntity;
            }
        }

        return null;
    }

    public static void saveToFile() {
        try {
            Files.writeString(SAVE_PATH, GSON.toJson(bonfireData));
        } catch (IOException e) {
            MiACompat.LOGGER.error("[MiACompat] Failed to save bonfire data!", e);
        }
    }

    public static void loadFromFile() {
        if (!Files.exists(SAVE_PATH)) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(SAVE_PATH)).getAsJsonObject();
            BonfireData data = GSON.fromJson(root, BonfireData.class);
            if (data != null) {
                bonfireData = data;
                MiACompat.LOGGER.info("[MiACompat] Loaded last bonfire at {} (set={})", bonfireData.getBlockPos(), bonfireData.isBonfireSet);
            }
        } catch (IOException e) {
            MiACompat.LOGGER.error("[MiACompat] Failed to load bonfire data!", e);
        }
    }
}
