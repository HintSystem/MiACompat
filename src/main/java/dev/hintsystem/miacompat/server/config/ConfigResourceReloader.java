package dev.hintsystem.miacompat.server.config;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.InventoryTracker;
import dev.hintsystem.miacompat.server.ServerItemRegistry;
import dev.hintsystem.miacompat.server.ServerMobRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import org.slf4j.helpers.MessageFormatter;

public class ConfigResourceReloader implements ResourceManagerReloadListener {
    public static boolean isYamlResource(Identifier resourceId) {
        return resourceId.getNamespace().equals(MiACompat.MOD_ID)
            && resourceId.getPath().endsWith(".yml");
    }

    public static final class Stopwatch implements AutoCloseable {
        private final String message;
        private Object[] args;
        private String timeFormat = "(%.2f ms)";

        private final long start;

        private Stopwatch(String message) {
            this.message = message;
            this.start = System.nanoTime();
        }

        public static Stopwatch start(String message) {
            return new Stopwatch(message);
        }

        public Stopwatch args(Object ...args) {
            this.args = args;
            return this;
        }

        public Stopwatch timeFormat(String format) {
            this.timeFormat = format;
            return this;
        }

        public long elapsedNanos() { return System.nanoTime() - start; }

        public double elapsedMillis() { return elapsedNanos() / 1_000_000.0; }

        @Override
        public void close() {
            String formattedMessage = args != null
                ? MessageFormatter.arrayFormat(message, args).getMessage() : message;

            MiACompat.LOGGER.info("{} {}", formattedMessage, String.format(timeFormat, elapsedMillis()));
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        MiACompat.LOGGER.info("Loading Mine in Abyss config resources...");

        try (Stopwatch ignore = Stopwatch
            .start("Mine in Abyss configs loaded")
            .timeFormat("in %.2f ms")
        ) {
            ServerItemRegistry.loadFromResources(resourceManager);
            ServerMobRegistry.loadFromResources(resourceManager);
            InventoryTracker.loadFromResources(resourceManager);
        }
    }
}
