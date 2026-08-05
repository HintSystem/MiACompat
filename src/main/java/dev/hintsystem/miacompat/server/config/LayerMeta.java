package dev.hintsystem.miacompat.server.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public enum LayerMeta {
    Orth("orth", "layer_orth"),

    L1("layerone", "layer_1"),
    L2("layertwo", "layer_2"),
    L3("layerthree", "layer_3"),
    L4("layerfour", "layer_4", 0x567252, 0x70946B),
    L5("layerfive", "layer_5", 0x434868, 0x636B9C);

    private static final Map<String, LayerMeta> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(
            type -> type.id.toLowerCase(Locale.ROOT),
            Function.identity()
        ));

    public final String id;
    public final String spriteName;
    @Nullable
    public final Integer titleColor;
    @Nullable
    public final Integer subtitleColor;

    LayerMeta(String id, String spriteName) {
        this(id, spriteName, null, null);
    }

    LayerMeta(String id, String spriteName, Integer titleColor, Integer subtitleColor) {
        this.id = id;
        this.spriteName = spriteName;
        this.titleColor = titleColor;
        this.subtitleColor = subtitleColor;
    }

    @Nullable
    public static LayerMeta getById(String id) {
        if (id == null) return null;
        return BY_ID.get(id.toLowerCase(Locale.ROOT));
    }
}
