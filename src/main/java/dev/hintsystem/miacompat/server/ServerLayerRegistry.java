package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.config.ConfigResourceReloader.Stopwatch;
import dev.hintsystem.miacompat.server.config.LayerConfig;
import dev.hintsystem.miacompat.server.config.LayerMeta;
import dev.hintsystem.miacompat.server.config.LayerYamlSchema;
import dev.hintsystem.miacompat.server.config.SectionYamlSchema;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public class ServerLayerRegistry {
    private static final Map<String, SectionYamlSchema.Section> sectionConfigById = new HashMap<>();
    private static final Map<String, LayerConfig> layerConfigById = new HashMap<>();

    private static final Map<String, LayerConfig> layerConfigBySectionId = new HashMap<>();

    /** @param regionId layer id or section id */
    @Nullable
    public static LayerConfig getLayer(String regionId) {
        LayerConfig layer = layerConfigById.get(regionId);
        return layer != null ? layer : layerConfigBySectionId.get(regionId);
    }

    public static LayerConfig getLayer(SectionYamlSchema.Section section) {
        return layerConfigBySectionId.get(section.name);
    }

    @Nullable
    public static SectionYamlSchema.Section getSectionForPosition(Vec3i position) {
        for (SectionYamlSchema.Section section : sectionConfigById.values()) {
            if (section.region.contains(position))
                return section;
        }

        return null;
    }

    private static void resolveLayerSections() {
        layerConfigBySectionId.clear();

        Set<String> claimedSectionIds = new HashSet<>();

        for (LayerConfig layer : layerConfigById.values()) {
            for (int i = 0; i < layer.sections.size(); i++) {
                String sectionId = layer.sections.get(i);
                SectionYamlSchema.Section section = sectionConfigById.get(sectionId);
                if (section == null) {
                    MiACompat.LOGGER.error("Layer '{}' references unknown section '{}'", layer.id, sectionId);
                    continue;
                }

                if (layerConfigBySectionId.putIfAbsent(sectionId, layer) != null) {
                    MiACompat.LOGGER.error("Section '{}' is assigned to multiple layers", sectionId);
                    continue;
                }

                claimedSectionIds.add(sectionId);
            }
        }

        for (String sectionId : sectionConfigById.keySet()) {
            if (!claimedSectionIds.contains(sectionId)) {
                MiACompat.LOGGER.error("Section '{}' does not belong to any layer", sectionId);
            }
        }
    }

    private static void registerLayer(LayerYamlSchema.Layer layer) {
        LayerMeta meta = LayerMeta.getById(layer.id);
        if (meta == null) {
            MiACompat.LOGGER.error("Loaded unknown layer '{}' with no matching LayerMeta enum", layer.id);
            return;
        }

        if (layerConfigById.putIfAbsent(layer.id, new LayerConfig(layer, meta)) != null)
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

            resolveLayerSections();

            sw.args(sectionConfigById.size(), layerConfigById.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sections and layers", e);
        }
    }
}
