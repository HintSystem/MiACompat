package dev.hintsystem.miacompat.server.config;

import dev.hintsystem.miacompat.server.config.yaml.ScalarConstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

public class SectionYamlSchema {
    public List<Section> sections;

    public static class Section {
        public String name;
        public String world;

        public Region region;

        public BlockPos refTop;
        public BlockPos refBottom;
    }

    public static class Region {
        public BlockPos start;
        public BlockPos end;

        private transient BoundingBox boundingBox;

        public boolean contains(Vec3i pos) {
            if (boundingBox == null)
                boundingBox = BoundingBox.fromCorners(start, end);

            return boundingBox.isInside(pos);
        }
    }

    public static Constructor constructor(LoaderOptions loaderOptions) {
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        Constructor constructor = new ScalarConstructor(SectionYamlSchema.class, loaderOptions)
            .addStringParser(BlockPos.class, StringParsers::parseBlockPos);

        constructor.setPropertyUtils(propertyUtils);

        return constructor;
    }
}
