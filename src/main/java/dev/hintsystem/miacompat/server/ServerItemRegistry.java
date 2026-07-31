package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.config.ConfigResourceReloader;
import dev.hintsystem.miacompat.server.config.ConfigResourceReloader.Stopwatch;
import dev.hintsystem.miacompat.server.config.geary.ItemYamlSchema;
import dev.hintsystem.miacompat.server.config.geary.item.ActionCooldown;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;
import dev.hintsystem.miacompat.server.config.geary.item.ItemCooldowns;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.utils.ItemUtil;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public class ServerItemRegistry {
    private static final Map<Identifier, ItemConfig> itemConfigByPrefabId = new HashMap<>();

    private static final Map<String, List<ActionCooldown>> actionCooldownByDisplay = new HashMap<>();
    private static final Set<String> actionFailMessages = new HashSet<>();

    public static Map<Identifier, ItemConfig> getAllItems() {
        return Collections.unmodifiableMap(itemConfigByPrefabId);
    }

    @Nullable
    public static ItemConfig getItem(ItemStack item) {
        Set<Identifier> prefabs = GearyData.getPrefabIds(item);
        if (prefabs.isEmpty()) return null;

        if (prefabs.size() > 1)
            MiACompat.LOGGER.warn("Item {} has multiple prefabs: {}", ItemUtil.itemDescriptor(item), prefabs);

        return getItem(prefabs.iterator().next());
    }

    @Nullable
    public static ItemConfig getItem(Identifier prefabId) { return itemConfigByPrefabId.get(prefabId); }

    public static List<ActionCooldown> getActionCooldownsByDisplay(String display) {
        if (display == null) return List.of();
        List<ActionCooldown> cooldowns = actionCooldownByDisplay.get(display);

        return cooldowns != null ? cooldowns : List.of();
    }

    public static boolean isActionFailMessage(String message) {
        return actionFailMessages.contains(message);
    }

    private static void registerItem(ItemConfig item) {
        ItemConfig prev = itemConfigByPrefabId.putIfAbsent(item.prefabId, item);
        if (prev != null)
            MiACompat.LOGGER.warn("Item {} already registered with prefab id '{}'",
                ItemUtil.itemDescriptor(item), item.prefabId);

        if (item.itemCooldowns != null) registerItemCooldowns(item.itemCooldowns);
    }

    private static void registerItemCooldowns(ItemCooldowns cooldowns) {
        registerActionCooldown(cooldowns.leftClick);
        registerActionCooldown(cooldowns.rightClick);
    }

    private static void registerActionCooldown(ActionCooldown cooldown) {
        if (cooldown == null) return;

        if (cooldown.cooldownDisplay != null) {
            actionCooldownByDisplay
                .computeIfAbsent(cooldown.cooldownDisplay, (k) -> new ArrayList<>())
                .add(cooldown);
        }

        if (cooldown.failMessage != null) {
            actionFailMessages.add(cooldown.failMessage);
        }
    }

    public static void loadFromResources(ResourceManager resourceManager) {
        itemConfigByPrefabId.clear();

        actionCooldownByDisplay.clear();
        actionFailMessages.clear();

        Yaml yaml = new Yaml(ItemYamlSchema.constructor(new LoaderOptions()));

        try (Stopwatch sw = Stopwatch.start("Loaded {} items")) {
            String itemConfigPath = "config/server/geary/prefabs";
            resourceManager.listResources(itemConfigPath, ConfigResourceReloader::isYamlResource)
                .forEach((id, resource) -> {
                    try (InputStream is = resource.open()) {
                        ItemYamlSchema itemConfig = yaml.load(is);

                        Path relative = Path.of(itemConfigPath).relativize(
                            Path.of(id.getPath())
                        );

                        String prefabNamespace = relative.getName(0).toString();

                        String filename = relative.getFileName().toString();
                        String prefabName = filename.substring(0, filename.length() - ".yml".length());

                        Identifier prefabId = Identifier.fromNamespaceAndPath(
                            prefabNamespace, prefabName
                        );

                        ItemConfig item = ItemConfig.parse(prefabId, itemConfig);

                        if (relative.startsWith("relics")) {
                            item = RelicConfig.parse(item);
                        } else {
                            ItemConfig parsed = RelicConfig.tryParse(item);
                            if (parsed != null) item = parsed;
                        }

                        registerItem(item);
                    } catch (Exception e) {
                        MiACompat.LOGGER.error("Failed to load item config '{}'", id, e);
                    }
                });

            sw.args(itemConfigByPrefabId.size());
        }
    }
}
