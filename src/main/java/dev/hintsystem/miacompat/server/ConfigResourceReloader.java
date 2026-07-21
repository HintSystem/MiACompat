package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.InventoryTracker;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ConfigResourceReloader implements ResourceManagerReloadListener {
    public static boolean isYamlResource(Identifier resourceId) {
        return resourceId.getNamespace().equals(MiACompat.MOD_ID)
            && resourceId.getPath().endsWith(".yml");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        long start = System.nanoTime();
        MiACompat.LOGGER.info("Loading Mine in Abyss config resources...");

        ServerItemRegistry.loadFromResources(resourceManager);
        ServerMobRegistry.loadFromResources(resourceManager);
        InventoryTracker.loadFromResources(resourceManager);

        long elapsed = System.nanoTime() - start;
        MiACompat.LOGGER.info(
            "Mine in Abyss configs loaded in {} ms",
            elapsed / 1_000_000.0
        );
    }
}
