package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.config.PersistentGsonData;
import dev.hintsystem.miacompat.server.ServerItemRegistry;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.utils.ItemUtils;

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

    private static final CompendiumData compendiumData = new CompendiumData();
    private static int relicCheckTicks = 0;

    public static void tick(Minecraft client) {
        if (client.player == null) return;

        ProfilerFiller profiler = Profiler.get();
        profiler.push("scanDiscoveredRelics");

        relicCheckTicks = (relicCheckTicks + 1) % SCAN_DISCOVERED_RELICS_TICKS;
        if (relicCheckTicks != 0)
            scanDiscoveredRelics(client.player);

        profiler.pop();
    }

    private static void scanDiscoveredRelics(LocalPlayer player) {
        for (ItemStack itemStack : ItemUtils.iterateContainedItems(player.getInventory())) {
            ItemConfig item = ServerItemRegistry.getItem(itemStack);
            if (!(item instanceof RelicConfig relic)) continue;

            if (compendiumData.relics.add(relic.prefabId))
                compendiumData.saveToFile();
        }
    }

    public static boolean isRelicDiscovered(Identifier prefabId) {
        return compendiumData.relics.contains(prefabId);
    }

    public static void loadFromFile() {
        compendiumData.loadFromFile();
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
        }

        @Override
        protected Class<CompendiumData> getDataClass() { return CompendiumData.class; }
    }
}
