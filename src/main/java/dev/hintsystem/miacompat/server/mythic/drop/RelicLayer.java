package dev.hintsystem.miacompat.server.mythic.drop;

import dev.hintsystem.miacompat.server.ServerMobRegistry;
import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RelicLayer {
    L1("prayingskeleton_l1", MiaDeeperWorld.LayerInfo.L1),
    L2("prayingskeleton_l2", MiaDeeperWorld.LayerInfo.L2),
    L3("prayingskeleton_l3", MiaDeeperWorld.LayerInfo.L3),
    L4("prayingskeleton_l4", MiaDeeperWorld.LayerInfo.L4),
    L5("prayingskeleton_l5", MiaDeeperWorld.LayerInfo.L5);

    public final String mobId;
    public final MiaDeeperWorld.LayerInfo info;

    RelicLayer(String relicDropMobId, MiaDeeperWorld.LayerInfo info) {
        this.mobId = relicDropMobId;
        this.info = info;
    }

    private static final Map<String, RelicLayer> BY_MOB =
        Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                l -> l.mobId,
                Function.identity()
            ));

    public static Optional<RelicLayer> fromMobDrop(ServerMobRegistry.MobDrop<?> drop) {
        return fromMobId(drop.mobId());
    }

    public static Optional<RelicLayer> fromMobId(String id) {
        return Optional.ofNullable(BY_MOB.get(id.toLowerCase(Locale.ROOT)));
    }
}
