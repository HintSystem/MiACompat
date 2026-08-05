package dev.hintsystem.miacompat.server.config.mythic.drop;

import dev.hintsystem.miacompat.server.config.LayerMeta;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RelicLayer {
    L1("prayingskeleton_l1", LayerMeta.L1),
    L2("prayingskeleton_l2", LayerMeta.L2),
    L3("prayingskeleton_l3", LayerMeta.L3),
    L4("prayingskeleton_l4", LayerMeta.L4),
    L5("prayingskeleton_l5", LayerMeta.L5);

    public final String mobId;
    public final LayerMeta meta;

    RelicLayer(String relicDropMobId, LayerMeta meta) {
        this.mobId = relicDropMobId;
        this.meta = meta;
    }

    private static final Map<String, RelicLayer> BY_MOB =
        Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                l -> l.mobId,
                Function.identity()
            ));

    public static Optional<RelicLayer> fromMobDrop(MobDrop<?> drop) {
        return fromMobId(drop.mob().id);
    }

    public static Optional<RelicLayer> fromMobId(String id) {
        return Optional.ofNullable(BY_MOB.get(id.toLowerCase(Locale.ROOT)));
    }
}
