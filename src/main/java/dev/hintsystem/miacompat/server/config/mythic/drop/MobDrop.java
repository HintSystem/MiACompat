package dev.hintsystem.miacompat.server.config.mythic.drop;

import dev.hintsystem.miacompat.server.config.mythic.mob.MobConfig;

public record MobDrop<T extends DropEntry>(MobConfig mob, T drop) {
    public <U extends DropEntry> MobDrop<U> withDrop(U drop) {
        return new MobDrop<>(mob, drop);
    }
}
