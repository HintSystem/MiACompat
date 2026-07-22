package dev.hintsystem.miacompat.server.mythic.drop;

import dev.hintsystem.miacompat.server.mythic.MythicParser;
import dev.hintsystem.miacompat.server.mythic.SkillEntry;

import net.minecraft.resources.Identifier;

import java.util.EnumSet;
import java.util.Map;

public final class RelicDrop extends ItemDrop implements DropEntry {
    public final SkillEntry dropSkill;

    public RelicDrop(
        Identifier prefabId, SkillEntry dropSkill,
        MythicParser.IntRange amount, double chance,
        EnumSet<DropFlag> flags
    ) {
        super(prefabId, Map.of(), amount, chance, flags);
        this.dropSkill = dropSkill;
    }
}
