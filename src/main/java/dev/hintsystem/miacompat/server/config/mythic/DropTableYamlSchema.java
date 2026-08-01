package dev.hintsystem.miacompat.server.config.mythic;

import dev.hintsystem.miacompat.server.config.yaml.RootMapConstructor;

import java.util.LinkedHashMap;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

public class DropTableYamlSchema extends LinkedHashMap<String, DropTableYamlSchema.DropTable> {
    public static class DropTable {
        public Integer MaxItems;
        public Integer TotalItems;
        public List<String> Conditions;
        public List<String> Drops;
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        return new RootMapConstructor<>(
            DropTableYamlSchema::new, DropTableYamlSchema.class,
            DropTable.class,
            loaderOptions
        );
    }
}
