package dev.hintsystem.miacompat.server.config;

import dev.hintsystem.miacompat.server.MiniMessageParser;
import dev.hintsystem.miacompat.server.config.yaml.ScalarConstructor;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.time.Duration;
import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Tag;

public class LayerYamlSchema {
    public List<Layer> layers;

    public static class Layer {
        public String id;
        public Component name;
        public Component sub;
        public Component deathMessage;
        public Depth depth;

        public boolean hasPvpDefault = false;

        public List<Effect> effects;
        public List<String> sections;
    }

    public static class Depth {
        public int start;
        public int end;
    }

    public abstract static class Effect {
        public Duration offset;
        public Duration duration;
        public Integer iterations;
    }

    public static class PotionEffect extends Effect {
        public Integer strength;
        public List<Identifier> effects;
    }

    public static class MaxHealthEffect extends Effect {
        public Float addMaxHealth;
        public Float minHealth;
    }

    public static class ParticleEffect extends Effect {
        public Integer count;
        public List<String> particles;
    }

    public static class SoundEffect extends Effect {
        public Integer count;
        public List<String> sounds;
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        Constructor constructor = new ScalarConstructor(LayerYamlSchema.class, loaderOptions)
            .addStringParser(Component.class, LayerYamlSchema::parseComponent)
            .addStringParser(Identifier.class, StringParsers::parseIdentifier)
            .addStringParser(Duration.class, StringParsers::parseDuration);

        constructor.setPropertyUtils(propertyUtils);

        constructor.addTypeDescription(new TypeDescription(
            LayerYamlSchema.PotionEffect.class, new Tag("potion")));

        constructor.addTypeDescription(new TypeDescription(
            LayerYamlSchema.MaxHealthEffect.class, new Tag("maxHealth")));

        constructor.addTypeDescription(new TypeDescription(
            LayerYamlSchema.ParticleEffect.class, new Tag("particles")));

        constructor.addTypeDescription(new TypeDescription(
            LayerYamlSchema.SoundEffect.class, new Tag("sound")));

        return constructor;
    }

    public static Component parseComponent(String value) {
        return MiniMessageParser.parse(value);
    }
}
