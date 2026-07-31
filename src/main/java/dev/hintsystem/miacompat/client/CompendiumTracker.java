package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.config.PersistentGsonData;
import dev.hintsystem.miacompat.server.ServerItemRegistry;
import dev.hintsystem.miacompat.server.ServerMobRegistry;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.utils.ItemUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

public class CompendiumTracker {
    public static final int SCAN_DISCOVERED_RELICS_TICKS = 48;
    public static final int SAVE_DATA_DELAY_TICKS = 100;

    private static final CompendiumData compendiumData = new CompendiumData();

    private static int relicCheckTicks = 0;
    private static int saveDelayTicks = 0;

    private static boolean dirty = false;

    public static void tick(Minecraft client) {
        if (client.player == null) return;

        ProfilerFiller profiler = Profiler.get();
        profiler.push("scanDiscoveredRelics");

        if (relicCheckTicks++ >= SCAN_DISCOVERED_RELICS_TICKS) {
            relicCheckTicks = 0;
            scanDiscoveredRelics(client.player);
        }

        profiler.pop();

        if (dirty && saveDelayTicks-- <= 0)
            saveIfDirty();
    }

    private static void scanDiscoveredRelics(LocalPlayer player) {
        for (ItemStack itemStack : ItemUtil.iterateContainedItems(player.getInventory())) {
            ItemConfig item = ServerItemRegistry.getItem(itemStack);
            if (!(item instanceof RelicConfig relic)) continue;

            addDiscoveredRelic(relic);
        }
    }

    public static boolean isRelicDiscovered(Identifier prefabId) {
        return compendiumData.relics.contains(prefabId);
    }

    /** @return {@code true}, if relic has not been seen before */
    public static boolean addDiscoveredRelic(RelicConfig relic) {
        if (!compendiumData.relics.add(relic.prefabId))
            return false;

        markDirty();
        return true;
    }

    public static void addKilledMob(String mobModelId) {
        if (!ServerMobRegistry.isExistingMobModel(mobModelId)) {
            MiACompat.LOGGER.warn("Tried to add unregistered mob model '{}' to compendium", mobModelId);
            return;
        }

        compendiumData.mobs.merge(mobModelId, 1, Integer::sum);
        markDirty();
    }

    public static void markDirty() {
        dirty = true;
        saveDelayTicks = SAVE_DATA_DELAY_TICKS;
    }

    public static void loadFromFile() { compendiumData.loadFromFile(); }

    public static void saveIfDirty() {
        if (!dirty) return;

        compendiumData.saveToFile();
        dirty = false;
    }

    public static class CompendiumData extends PersistentGsonData<CompendiumData> {
        public Set<Identifier> relics = new HashSet<>();
        public Map<String, Integer> mobs = new HashMap<>();

        @Override
        protected Gson getGson() {
            return DEFAULT_GSON.newBuilder()
                .setPrettyPrinting()
                .create();
        }

        @Override
        public String getDataTitle() { return "MiACompat compendium discoveries"; }

        @Override
        public Path getFilePath() { return MiACompat.CONFIG_FOLDER.resolve("compendium.json"); }

        @Override
        public Path getBackupFolder() { return MiACompat.CONFIG_FOLDER; }

        @Override
        protected void applyData(CompendiumData data) {
            relics.addAll(data.relics);

            data.mobs.forEach((mob, count) ->
                mobs.merge(mob, count, Math::max)
            );
        }

        @Override
        protected Class<CompendiumData> getDataClass() { return CompendiumData.class; }
    }
}
