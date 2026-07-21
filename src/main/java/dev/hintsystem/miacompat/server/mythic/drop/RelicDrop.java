package dev.hintsystem.miacompat.server.mythic.drop;

import dev.hintsystem.miacompat.server.mythic.SkillEntry;
import net.minecraft.resources.Identifier;

public record RelicDrop(
    SkillEntry dropSkill, Identifier prefabId,
    double chance
) {}
