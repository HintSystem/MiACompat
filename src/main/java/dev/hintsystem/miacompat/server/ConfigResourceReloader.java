package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.InventoryTracker;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ConfigResourceReloader implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        long start = System.nanoTime();
        MiACompat.LOGGER.info("Loading Mine in Abyss config resources...");

        ServerItemRegistry.loadFromResources(resourceManager);
        InventoryTracker.loadFromResources(resourceManager);

        long elapsed = System.nanoTime() - start;
        MiACompat.LOGGER.info(
            "Mine in Abyss configs loaded in {} ms",
            elapsed / 1_000_000.0
        );
    }
}
