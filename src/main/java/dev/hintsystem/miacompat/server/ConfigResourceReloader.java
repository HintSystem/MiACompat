package dev.hintsystem.miacompat.server;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ConfigResourceReloader implements ResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        ServerItemRegistry.loadFromResources(resourceManager);
    }
}
