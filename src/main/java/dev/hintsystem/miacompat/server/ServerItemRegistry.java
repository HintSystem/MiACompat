package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.CooldownTracker;
import dev.hintsystem.miacompat.server.config.ConfigResourceReloader;
import dev.hintsystem.miacompat.server.config.geary.ItemYamlSchema;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.utils.ItemUtils;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public class ServerItemRegistry {
    private static final Map<Identifier, ItemConfig> itemConfigByPrefabId = new HashMap<>();

    public static Map<Identifier, ItemConfig> getAllItems() {
        return Collections.unmodifiableMap(itemConfigByPrefabId);
    }

    @Nullable
    public static ItemConfig getItem(ItemStack item) {
        Set<Identifier> prefabs = GearyData.getPrefabIds(item);
        if (prefabs.isEmpty()) return null;

        if (prefabs.size() > 1)
            MiACompat.LOGGER.warn("Item {} has multiple prefabs: {}", ItemUtils.itemDescriptor(item), prefabs);

        return getItem(prefabs.iterator().next());
    }

    @Nullable
    public static ItemConfig getItem(Identifier prefabId) { return itemConfigByPrefabId.get(prefabId); }

    private static void registerItem(ItemConfig item) {
        ItemConfig prev = itemConfigByPrefabId.putIfAbsent(item.prefabId, item);
        if (prev != null)
            MiACompat.LOGGER.warn("Item {} already registered with prefab id '{}'",
                ItemUtils.itemDescriptor(item), item.prefabId);

        if (item.gearCooldowns != null) CooldownTracker.registerGearCooldowns(item.gearCooldowns);
    }

    public static void loadFromResources(ResourceManager resourceManager) {
        itemConfigByPrefabId.clear();

        Yaml yaml = new Yaml(ItemYamlSchema.constructor(new LoaderOptions()));

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

        MiACompat.LOGGER.info("Loaded {} items", itemConfigByPrefabId.size());
    }
}
