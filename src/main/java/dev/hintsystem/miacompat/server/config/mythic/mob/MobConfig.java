package dev.hintsystem.miacompat.server.config.mythic.mob;

import dev.hintsystem.miacompat.server.MiniMessageParser;
import dev.hintsystem.miacompat.server.config.mythic.MobYamlSchema;
import dev.hintsystem.miacompat.server.config.mythic.SkillEntry;
import dev.hintsystem.miacompat.server.config.mythic.drop.DropEntry;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public class MobConfig {
    public final String id;
    @Nullable public final String template;
    @Nullable public final String modelId;

    public final SpawnCategory spawnCategory;
    @Nullable public final Component display;

    public final MobYamlSchema.Options options;

    public final List<DropEntry> drops;
    public final List<SkillEntry> skills;

    public MobConfig(
        String id, @Nullable String template, @Nullable String modelId,
        SpawnCategory spawnCategory, @Nullable Component display,
        MobYamlSchema.Options options,
        List<DropEntry> drops, List<SkillEntry> skills
    ) {
        this.id = id;
        this.template = template;
        this.modelId = modelId;
        this.spawnCategory = spawnCategory;
        this.display = display;
        this.options = options;
        this.drops = drops;
        this.skills = skills;
    }

    public static MobConfig parse(String mobId, MobYamlSchema.MobDefinition mobConfig) throws Exception {
        String template = mobConfig.Template != null && !mobConfig.Template.isBlank()
            ? mobConfig.Template.toLowerCase(Locale.ROOT) : null;

        String modelId = mobConfig.Model != null
            ? mobConfig.Model.Id.toLowerCase(Locale.ROOT) : null;

        Component display = mobConfig.Display != null
            ? MiniMessageParser.parse(mobConfig.Display) : null;

        return new MobConfig(
            mobId.toLowerCase(Locale.ROOT), template, modelId,
            SpawnCategory.parse(mobConfig.SpawnCategory), display,
            mobConfig.Options,
            DropEntry.parseList(mobConfig.Drops), SkillEntry.parseList(mobConfig.Skills)
        );
    }

    public MobConfig inheritFrom(MobConfig template) {
        List<DropEntry> drops = new ArrayList<>(template.drops);
        drops.addAll(this.drops);

        List<SkillEntry> skills = new ArrayList<>(template.skills);
        skills.addAll(this.skills);

        return new MobConfig(
            this.id, this.template,
            this.modelId != null ? this.modelId : template.modelId,

            this.spawnCategory != null ? this.spawnCategory : template.spawnCategory,
            this.display != null ? this.display : template.display,
            this.options != null ? this.options.inheritFrom(template.options) : template.options,

            drops, skills
        );
    }
}
