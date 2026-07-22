package dev.hintsystem.miacompat.server.config.mythic.mob;

import dev.hintsystem.miacompat.server.MiniMessageParser;
import dev.hintsystem.miacompat.server.config.mythic.MobYamlSchema;
import dev.hintsystem.miacompat.server.config.mythic.SkillEntry;
import dev.hintsystem.miacompat.server.config.mythic.drop.DropEntry;

import net.minecraft.network.chat.Component;

import java.util.List;

public class MobConfig {
    public final String id;
    public final String template;

    public final SpawnCategory spawnCategory;
    public final Component display;

    public final MobYamlSchema.Options options;

    public final List<DropEntry> drops;
    public final List<SkillEntry> skills;

    public MobConfig(
        String id, String template,
        SpawnCategory spawnCategory, Component display, MobYamlSchema.Options options,
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

    public static MobConfig parse(String mobId, MobYamlSchema.MobDefinition mobConfig) throws Exception {
        return new MobConfig(
            mobId, mobConfig.Template,
            SpawnCategory.parse(mobConfig.SpawnCategory),
            mobConfig.Display != null ? MiniMessageParser.parse(mobConfig.Display) : null,
            mobConfig.Options,
            DropEntry.parseList(mobConfig.Drops), SkillEntry.parseList(mobConfig.Skills)
        );
    }
}
