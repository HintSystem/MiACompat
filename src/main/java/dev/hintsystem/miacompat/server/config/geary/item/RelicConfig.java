package dev.hintsystem.miacompat.server.config.geary.item;

import dev.hintsystem.miacompat.server.config.geary.ItemYamlSchema;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

public class RelicConfig extends ItemConfig {
    private static final Pattern GRADE_PATTERN =
        Pattern.compile("\\bGrade\\s+(I|II|III|IV)\\b");

    public final RelicGrade grade;

    RelicConfig(RelicGrade grade, ItemConfig itemConfig) {
        super(itemConfig.getOriginal(), itemConfig.prefabId, itemConfig.type, itemConfig.name, itemConfig.modelId, itemConfig.lore, itemConfig.gearCooldowns);
        this.grade = grade;
    }

    @Nullable
    private static RelicGrade extractGrade(String infoLine) {
        Matcher m = GRADE_PATTERN.matcher(infoLine);
        return m.find() ? RelicGrade.valueOf(m.group(1)) : null;
    }

    /**
     * @return null, if item is not a relic
     */
    @Nullable
    public static RelicConfig tryParse(ItemConfig item) {
        ItemYamlSchema.Item originalItem = item.getOriginal().getItem();
        if (originalItem.lore == null) return null;

        String infoLine = originalItem.lore.getFirst();
        RelicGrade grade = extractGrade(infoLine);

        return grade != null ? new RelicConfig(grade, item) : null;
    }

    public static RelicConfig parse(ItemConfig item) {
        String infoLine = item.getOriginal().getItem().lore.getFirst();
        RelicGrade grade = extractGrade(infoLine);

        if (grade == null)
            throw new IllegalStateException("Invalid relic grade: " + infoLine);

        return new RelicConfig(grade, item);
    }
}
