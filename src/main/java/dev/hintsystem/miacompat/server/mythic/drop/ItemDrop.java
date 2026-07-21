package dev.hintsystem.miacompat.server.mythic.drop;

import dev.hintsystem.miacompat.server.mythic.MythicParser;

import net.minecraft.resources.Identifier;

import java.util.EnumSet;
import java.util.Map;

public record ItemDrop(
    Identifier itemId, Map<String, String> arguments,
    MythicParser.IntRange amount, double chance,
    EnumSet<DropFlag> flags
) implements DropEntry {

    public enum DropFlag { NO_LOOTING }
}
