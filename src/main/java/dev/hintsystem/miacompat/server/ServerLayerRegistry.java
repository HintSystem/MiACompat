package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.config.ConfigResourceReloader.Stopwatch;
import dev.hintsystem.miacompat.server.config.LayerYamlSchema;
import dev.hintsystem.miacompat.server.config.SectionYamlSchema;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public class ServerLayerRegistry {
    private static final Map<String, SectionYamlSchema.Section> sectionConfigById = new HashMap<>();
    private static final Map<String, LayerYamlSchema.Layer> layerConfigById = new HashMap<>();

    @Nullable
    public static LayerYamlSchema.Layer getLayer(String layerId) {
        return layerConfigById.get(layerId);
    }

    @Nullable
    public static SectionYamlSchema.Section getSectionForPosition(Vec3i position) {
        for (SectionYamlSchema.Section section : sectionConfigById.values()) {
            if (section.region.contains(position))
                return section;
        }

        return null;
    }

    private static void registerLayer(LayerYamlSchema.Layer layer) {
        LayerYamlSchema.Layer prev = layerConfigById.putIfAbsent(layer.id, layer);
        if (prev != null)
            MiACompat.LOGGER.error("Layer '{}' already registered", layer.id);
    }

    private static void registerSection(SectionYamlSchema.Section section) {
        SectionYamlSchema.Section prev = sectionConfigById.putIfAbsent(section.name, section);
        if (prev != null)
            MiACompat.LOGGER.error("Section '{}' already registered", section.name);
    }

    public static void loadFromResources(ResourceManager resourceManager) {
        sectionConfigById.clear();
        layerConfigById.clear();

        Identifier sectionConfigId = MiACompat.id("config/server/deeperworld/config.yml");
        Identifier layerConfigId = MiACompat.id("config/server/mineinabyss/layers.yml");

        LoaderOptions options = new LoaderOptions();
        Yaml sectionYaml = new Yaml(SectionYamlSchema.constructor(options));
        Yaml layerYaml = new Yaml(LayerYamlSchema.constructor(options));

        try (Stopwatch sw = Stopwatch.start("Loaded {} sections and {} layers")) {

            try (InputStream stream = resourceManager.getResourceOrThrow(sectionConfigId).open()) {
                SectionYamlSchema sectionSchema = sectionYaml.load(stream);

                for (SectionYamlSchema.Section sectionConfig : sectionSchema.sections) {
                    registerSection(sectionConfig);
                }
            }

            try (InputStream stream = resourceManager.getResourceOrThrow(layerConfigId).open()) {
                LayerYamlSchema layerSchema = layerYaml.load(stream);

                for (LayerYamlSchema.Layer layerConfig : layerSchema.layers) {
                    registerLayer(layerConfig);
                }
            }

            sw.args(sectionConfigById.size(), layerConfigById.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sections and layers", e);
        }
    }
}
