package dev.hintsystem.miacompat.server.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

public class LayerConfig {
    public final String id;
    public final Component name;
    public final Component subtitle;

    public boolean hasPvpDefault;

    public List<LayerYamlSchema.Effect> effects;
    public List<String> sections;

    public final LayerMeta meta;

    public LayerConfig(LayerYamlSchema.Layer layer, LayerMeta meta) {
        this.id = layer.id;
        this.name = layer.name;
        this.subtitle = layer.sub;
        this.hasPvpDefault = layer.hasPvpDefault;
        this.effects = layer.effects;
        this.sections = layer.sections;
        this.meta = meta;
    }

    /** @return index of this section, or {@code -1} if not part of this layer */
    public int getSectionIndex(SectionYamlSchema.Section section) {
        for (int i = 0; i < sections.size(); i ++) {
            if (sections.get(i).equals(section.name)) return i;
        }

        return -1;
    }

    public Integer getTitleColor() {
        return meta.titleColor != null ? meta.titleColor : getStyleColor(name.getStyle());
    }

    public Integer getSubtitleColor() {
        return meta.subtitleColor != null ? meta.subtitleColor : getStyleColor(subtitle.getStyle());
    }

    private Integer getStyleColor(Style style) {
        return style.getColor() != null ? style.getColor().getValue() : null;
    }
}
