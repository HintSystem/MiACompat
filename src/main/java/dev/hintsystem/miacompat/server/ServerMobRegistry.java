package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.mythic.SkillEntry;
import dev.hintsystem.miacompat.server.mythic.drop.DropEntry;
import dev.hintsystem.miacompat.server.mythic.drop.DropTableReference;
import dev.hintsystem.miacompat.server.mythic.drop.ItemDrop;
import dev.hintsystem.miacompat.server.mythic.drop.RelicDrop;
import dev.hintsystem.miacompat.server.schema.DropTableConfigSchema;
import dev.hintsystem.miacompat.server.schema.MobConfigSchema;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public class ServerMobRegistry {
    private static final Map<String, DropTableConfig> dropTableConfigById = new HashMap<>();
    private static final Map<String, MobConfig> mobConfigById = new HashMap<>();

    private static final Map<String, List<MobConfig>> mobConfigsByTemplate = new HashMap<>();

    public static Map<String, MobConfig> getAllMobs() {
        return Collections.unmodifiableMap(mobConfigById);
    }

    @Nullable
    public static MobConfig getMob(String id) {
        return mobConfigById.get(id.toLowerCase(Locale.ROOT));
    }

    public static List<MobConfig> getMobsWithTemplate(String template) {
        return Collections.unmodifiableList(
            mobConfigsByTemplate.getOrDefault(template.toLowerCase(Locale.ROOT), List.of())
        );
    }

    public static List<MobDrop<?>> resolveDrops(MobConfig mobConfig) {
        List<MobDrop<?>> resolvedDrops = new ArrayList<>();
        for (DropEntry entry : mobConfig.drops) {
            if (entry instanceof DropTableReference reference) {
                for (DropEntry drop : resolveDropTable(reference.tableName())) {
                    resolvedDrops.add(new MobDrop<>(mobConfig.id, drop));
                }
            } else {
                resolvedDrops.add(new MobDrop<>(mobConfig.id, entry));
            }
        }

        resolvedDrops.addAll(resolveRelicDrops(mobConfig));
        return resolvedDrops;
    }

    public static List<MobDrop<RelicDrop>> resolveRelicDrops(MobConfig mobConfig) {
        List<MobDrop<RelicDrop>> resolvedDrops = new ArrayList<>();
        for (SkillEntry skillEntry : mobConfig.skills) {
            if (!"relicDrop".equals(skillEntry.customSkillName()))
                continue;

            String dropTableName = skillEntry.mechanic().arguments().get("item");
            if (dropTableName == null) {
                MiACompat.LOGGER.warn("Relic drop has no drop table in mob '{}'", mobConfig.id);
                continue;
            }

            if (skillEntry.chance() == null) {
                MiACompat.LOGGER.warn("Relic drop '{}' has no drop chance in mob '{}'", dropTableName, mobConfig.id);
                continue;
            }

            for (DropEntry dropEntry : resolveDropTable(dropTableName)) {
                if (!(dropEntry instanceof ItemDrop itemDrop)) continue;

                double dropChance = skillEntry.chance() * itemDrop.chance;
                resolvedDrops.add(new MobDrop<>(
                    mobConfig.id,
                    new RelicDrop(
                        itemDrop.itemId, skillEntry, itemDrop.amount, dropChance, itemDrop.flags
                    )
                ));
            }
        }

        return resolvedDrops;
    }

    public record MobDrop<T extends DropEntry>(String mobId, T drop) {
        public <U extends DropEntry> MobDrop<U> withDrop(U drop) {
            return new MobDrop<>(mobId, drop);
        }
    }

    public static List<DropEntry> resolveDropTable(String dropTableName) {
        return resolveDropTable(dropTableName, new HashSet<>());
    }

    private static List<DropEntry> resolveDropTable(
        String dropTableName,
        Set<String> visited
    ) {
        String id = dropTableName.toLowerCase(Locale.ROOT);

        if (!visited.add(id)) {
            MiACompat.LOGGER.warn("Circular drop table reference detected: {}", id);
            return List.of();
        }

        DropTableConfig dropTableConfig = dropTableConfigById.get(id);
        if (dropTableConfig == null) {
            MiACompat.LOGGER.warn("Unknown drop table '{}'", dropTableName);
            return List.of();
        }

        List<DropEntry> resolved = new ArrayList<>();

        for (DropEntry entry : dropTableConfig.drops) {
            if (entry instanceof DropTableReference reference) {
                resolved.addAll(resolveDropTable(reference.tableName(), visited));
            } else {
                resolved.add(entry);
            }
        }

        return resolved;
    }

    public enum SpawnCategory {
        NONE(null),
        PASSIVE("passive"),
        HOSTILE("hostile"),
        FLYING("flying"),
        SWARM("swarm"),
        WATER("water"),
        MINI_BOSS("miniboss"),
        UNCOMMON("uncommon"),
        SPECIAL("special");

        private static final Map<String, SpawnCategory> BY_VALUE =
            Arrays.stream(values())
                .filter(c -> c.value != null)
                .collect(Collectors.toUnmodifiableMap(
                    c -> c.value.toLowerCase(Locale.ROOT),
                    Function.identity()
                ));

        public final String value;

        SpawnCategory(String value) {
            this.value = value;
        }

        public static SpawnCategory parse(String value) {
            if (value == null || value.isBlank()) return NONE;

            SpawnCategory category = BY_VALUE.get(value.toLowerCase(Locale.ROOT));
            if (category == null)
                throw new IllegalArgumentException("Unknown spawn category: " + value);

            return category;
        }
    }

    public static class MobConfig {
        public final String id;
        public final String template;

        public final SpawnCategory spawnCategory;
        public final Component display;

        public final MobConfigSchema.Options options;

        public final List<DropEntry> drops;
        public final List<SkillEntry> skills;

        public MobConfig(
            String id, String template,
            SpawnCategory spawnCategory, Component display, MobConfigSchema.Options options,
            List<DropEntry> drops, List<SkillEntry> skills
        ) {
            this.id = id;
            this.template = template;
            this.spawnCategory = spawnCategory;
            this.display = display;
            this.options = options;
            this.drops = drops;
            this.skills = skills;
        }

        private static MobConfig parse(String mobId, MobConfigSchema.MobDefinition mobConfig) throws Exception {
            return new MobConfig(
                mobId, mobConfig.Template,
                SpawnCategory.parse(mobConfig.SpawnCategory),
                mobConfig.Display != null ? MiniMessageParser.parse(mobConfig.Display) : null,
                mobConfig.Options,
                DropEntry.parseList(mobConfig.Drops), SkillEntry.parseList(mobConfig.Skills)
            );
        }
    }

    public static class DropTableConfig {
        public final String id;
        public final List<DropEntry> drops;

        public DropTableConfig(String id, List<DropEntry> drops) {
            this.id = id;
            this.drops = drops;
        }

        private static DropTableConfig parse(String dropTableId, DropTableConfigSchema.DropTableDefinition dropTableConfig) {
            return new DropTableConfig(dropTableId, DropEntry.parseList(dropTableConfig.Drops));
        }
    }

    public static void buildIndexes() {
        mobConfigsByTemplate.clear();

        for (MobConfig mob : mobConfigById.values()) {
            if (mob.template == null || mob.template.isBlank())
                continue;

            String template = mob.template.toLowerCase(Locale.ROOT);
            mobConfigsByTemplate
                .computeIfAbsent(template, k -> new ArrayList<>())
                .add(mob);
        }
    }

    private static void registerMob(MobConfig mob) {
        MobConfig prev = mobConfigById.putIfAbsent(mob.id.toLowerCase(Locale.ROOT), mob);
        if (prev != null)
            MiACompat.LOGGER.warn("Mob {} already registered", mob.id);
    }

    private static void registerDropTable(DropTableConfig dropTable) {
        DropTableConfig prev = dropTableConfigById.putIfAbsent(dropTable.id.toLowerCase(Locale.ROOT), dropTable);
        if (prev != null)
            MiACompat.LOGGER.warn("Drop table {} already registered", dropTable.id);
    }

    public static void loadFromResources(ResourceManager resourceManager) {
        dropTableConfigById.clear();
        mobConfigById.clear();

        LoaderOptions options = new LoaderOptions();
        Yaml dropTableYaml = new Yaml(DropTableConfigSchema.constructor(options));

        resourceManager.listResources("config/server/droptables", ConfigResourceReloader::isYamlResource)
            .forEach((id, resource) -> {
                try (InputStream is = resource.open()) {
                    DropTableConfigSchema dropTableConfig = dropTableYaml.load(is);

                    for (Map.Entry<String, DropTableConfigSchema.DropTableDefinition> entry : dropTableConfig.entrySet()) {
                        DropTableConfig dropTable = DropTableConfig.parse(entry.getKey(), entry.getValue());
                        registerDropTable(dropTable);
                    }
                } catch (Exception e) {
                    MiACompat.LOGGER.error("Failed to load drop table config '{}'", id, e);
                }
            });

        MiACompat.LOGGER.info("Loaded {} drop tables", dropTableConfigById.size());

        Yaml mobYaml = new Yaml(MobConfigSchema.constructor(options));

        resourceManager.listResources("config/server/mobs", ConfigResourceReloader::isYamlResource)
            .forEach((id, resource) -> {
                try (InputStream is = resource.open()) {
                    MobConfigSchema mobConfig = mobYaml.load(is);

                    for (Map.Entry<String, MobConfigSchema.MobDefinition> entry : mobConfig.entrySet()) {
                        MobConfig mob = MobConfig.parse(entry.getKey(), entry.getValue());
                        registerMob(mob);
                    }
                } catch (Exception e) {
                    MiACompat.LOGGER.error("Failed to load mob config '{}'", id, e);
                }
            });

        MiACompat.LOGGER.info("Loaded {} mobs", mobConfigById.size());

        buildIndexes();
    }
}
