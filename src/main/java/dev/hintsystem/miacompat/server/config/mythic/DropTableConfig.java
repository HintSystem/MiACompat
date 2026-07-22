package dev.hintsystem.miacompat.server.config.mythic;

import dev.hintsystem.miacompat.server.config.mythic.drop.DropEntry;

import java.util.List;

public class DropTableConfig {
    public final String id;
    public final List<DropEntry> drops;

    public DropTableConfig(String id, List<DropEntry> drops) {
        this.id = id;
        this.drops = drops;
    }

    public static DropTableConfig parse(String dropTableId, DropTableYamlSchema.DropTableDefinition dropTableConfig) {
        return new DropTableConfig(dropTableId, DropEntry.parseList(dropTableConfig.Drops));
    }
}
