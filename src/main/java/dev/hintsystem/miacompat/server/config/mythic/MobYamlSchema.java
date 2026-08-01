package dev.hintsystem.miacompat.server.config.mythic;

import dev.hintsystem.miacompat.server.config.yaml.RootMapConstructor;

import java.util.LinkedHashMap;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

public class MobYamlSchema extends LinkedHashMap<String, MobYamlSchema.Mob> {
    public static class Mob {
        public String Template;
        public String Type;
        public String SpawnCategory;
        public String Display;

        public Integer Health;
        public Integer Damage;

        public Options Options;
        public Model Model;

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

        public Options inheritFrom(Options parent) {
            if (parent == null) return this;

            Options o = new Options();

            o.FollowRange = inherit(FollowRange, parent.FollowRange);
            o.MaxCombatDistance = inherit(MaxCombatDistance, parent.MaxCombatDistance);
            o.KnockbackResistance = inherit(KnockbackResistance, parent.KnockbackResistance);
            o.PreventSunburn = inherit(PreventSunburn, parent.PreventSunburn);
            o.PreventOtherDrops = inherit(PreventOtherDrops, parent.PreventOtherDrops);
            o.Collidable = inherit(Collidable, parent.Collidable);

            return o;
        }

        private static <T> T inherit(T child, T parent) {
            return child != null ? child : parent;
        }
    }

    public static class Model {
        public String Id;
        public Integer ViewRadius;
        public Boolean DamageTint;
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        Constructor constructor = new RootMapConstructor<>(
            MobYamlSchema::new, MobYamlSchema.class,
            Mob.class,
            loaderOptions
        );
        constructor.setPropertyUtils(propertyUtils);

        return constructor;
    }
}
