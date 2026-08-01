package dev.hintsystem.miacompat.server.config.mythic;

import dev.hintsystem.miacompat.server.config.mythic.drop.DropEntry;

import java.util.List;
import java.util.Locale;

public class DropTableConfig {
    public final String id;
    public final List<DropEntry> drops;

    public DropTableConfig(String id, List<DropEntry> drops) {
        this.id = id;
        this.drops = drops;
    }

    public static DropTableConfig parse(String dropTableId, DropTableYamlSchema.DropTable dropTableConfig) {
        return new DropTableConfig(
            dropTableId.toLowerCase(Locale.ROOT),
            DropEntry.parseList(dropTableConfig.Drops)
        );
    }
}
