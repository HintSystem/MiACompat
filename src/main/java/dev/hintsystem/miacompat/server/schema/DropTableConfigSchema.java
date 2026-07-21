package dev.hintsystem.miacompat.server.schema;

import java.util.LinkedHashMap;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

public class DropTableConfigSchema extends LinkedHashMap<String, DropTableConfigSchema.DropTableDefinition> {
    public static class DropTableDefinition {
        public Integer MaxItems;
        public List<String> Drops;
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        return new RootMapConstructor<>(
            DropTableConfigSchema::new,
            DropTableConfigSchema.class,
            DropTableDefinition.class,
            loaderOptions
        );
    }
}
