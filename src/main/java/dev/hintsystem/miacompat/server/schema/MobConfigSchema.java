package dev.hintsystem.miacompat.server.schema;

import java.util.LinkedHashMap;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

public class MobConfigSchema extends LinkedHashMap<String, MobConfigSchema.MobDefinition> {
    public static class MobDefinition {
        public String Template;
        public String Type;
        public String SpawnCategory;
        public String Display;

        public Integer Health;
        public Integer Damage;

        public Options Options;
        public List<String> DamageModifiers;
        public List<String> Drops;
        public List<String> Skills;
    }

    public static class Options {
        public Integer FollowRange;
        public Integer MaxCombatDistance;
        public Double KnockbackResistance;
        public Boolean PreventSunburn;
        public Boolean PreventOtherDrops;
        public Boolean Collidable;
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        Constructor constructor = new RootMapConstructor<>(
            MobConfigSchema::new,
            MobConfigSchema.class,
            MobDefinition.class,
            loaderOptions
        );
        constructor.setPropertyUtils(propertyUtils);

        return constructor;
    }
}
