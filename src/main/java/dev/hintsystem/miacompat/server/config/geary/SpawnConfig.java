package dev.hintsystem.miacompat.server.config.geary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpawnConfig {
    /** Only unique to the folder of the config file */
    public final String id;
    public final String mobId;
    public final String inherit;

    public final Float priority;
    public final Float chance;

    public final List<String> regions;

    public SpawnConfig(
        String id, String mobId,
        String inherit,
        Float priority, Float chance,
        List<String> regions
    ) {
        this.id = id;
        this.mobId = mobId;
        this.inherit = inherit;
        this.priority = priority;
        this.chance = chance;
        this.regions = regions;
    }

    public static SpawnConfig parse(String spawnId, SpawnsYamlSchema.Spawn spawnConfig) {
        String mobId = spawnConfig.type != null
            ? spawnConfig.type.getPath().toLowerCase(Locale.ROOT) : null;

        return new SpawnConfig(
            spawnId.toLowerCase(Locale.ROOT), mobId,
            spawnConfig.inherit,
            spawnConfig.priority, spawnConfig.chance,
            spawnConfig.regions
        );
    }

    public SpawnConfig inheritFrom(SpawnConfig inherit) {
        List<String> regions = new ArrayList<>(inherit.regions);
        if (this.regions != null) regions.addAll(this.regions);

        return new SpawnConfig(
            this.id, this.mobId, this.inherit,
            this.priority != null ? this.priority : inherit.priority,
            this.chance != null ? this.chance : inherit.chance,
            regions
        );
    }
}
