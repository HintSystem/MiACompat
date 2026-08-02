package dev.hintsystem.miacompat.server.config.mythic.mob;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public enum SpawnCategory {
    PASSIVE("passive"),
    HOSTILE("hostile"),
    FLYING("flying"),
    SWARM("swarm"),
    WATER("water"),
    MINI_BOSS("miniboss"),
    UNCOMMON("uncommon"),
    SPECIAL("special");

    private static final Map<String, SpawnCategory> BY_VALUE =
        Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                c -> c.value.toLowerCase(Locale.ROOT),
                Function.identity()
            ));

    public final String value;

    SpawnCategory(String value) {
        this.value = value;
    }

    @Nullable
    public static SpawnCategory parse(String value) {
        if (value == null) return null;

        SpawnCategory category = BY_VALUE.get(value.toLowerCase(Locale.ROOT));
        if (category == null)
            throw new IllegalArgumentException("Unknown spawn category: " + value);

        return category;
    }
}
