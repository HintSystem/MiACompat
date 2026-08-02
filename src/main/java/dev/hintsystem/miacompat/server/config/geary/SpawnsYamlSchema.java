package dev.hintsystem.miacompat.server.config.geary;

import dev.hintsystem.miacompat.server.config.StringParsers;
import dev.hintsystem.miacompat.server.config.yaml.RootMapConstructor;

import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

public class SpawnsYamlSchema extends LinkedHashMap<String, SpawnsYamlSchema.Spawn> {
    public List<String> namespaces;

    public static class Spawn {
        public String inherit;
        public String position;
        public Identifier type;
        public Float priority;
        public Float chance;

        public List<String> regions;
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        RootMapConstructor<SpawnsYamlSchema, Spawn> constructor = new RootMapConstructor<>(
            SpawnsYamlSchema::new, SpawnsYamlSchema.class,
            Spawn.class,
            loaderOptions
        );
        constructor.addStringParser(Identifier.class, StringParsers::parseIdentifier);

        constructor.setPropertyUtils(propertyUtils);

        return constructor;
    }
}
