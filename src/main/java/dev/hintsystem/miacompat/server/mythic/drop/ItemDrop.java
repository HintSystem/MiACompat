package dev.hintsystem.miacompat.server.mythic.drop;

import dev.hintsystem.miacompat.server.mythic.MythicParser;

import net.minecraft.resources.Identifier;

import java.util.EnumSet;
import java.util.Map;

public non-sealed class ItemDrop implements DropEntry {
    public final Identifier itemId;
    public final Map<String, String> arguments;
    public final MythicParser.IntRange amount;
    public final double chance;
    public final EnumSet<DropFlag> flags;

    public ItemDrop(
        Identifier itemId, Map<String, String> arguments,
        MythicParser.IntRange amount, double chance,
        EnumSet<DropFlag> flags
    ) {
        this.itemId = itemId;
        this.arguments = arguments;
        this.amount = amount;
        this.chance = chance;
        this.flags = flags;
    }

    public enum DropFlag { NO_LOOTING }
}
