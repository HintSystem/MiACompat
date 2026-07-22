package dev.hintsystem.miacompat.server.config.mythic;

import dev.hintsystem.miacompat.server.config.RootMapYamlConstructor;

import java.util.LinkedHashMap;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

public class DropTableYamlSchema extends LinkedHashMap<String, DropTableYamlSchema.DropTableDefinition> {
    public static class DropTableDefinition {
        public Integer MaxItems;
        public List<String> Drops;
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        return new RootMapYamlConstructor<>(
            DropTableYamlSchema::new,
            DropTableYamlSchema.class,
            DropTableDefinition.class,
            loaderOptions
        );
    }
}
