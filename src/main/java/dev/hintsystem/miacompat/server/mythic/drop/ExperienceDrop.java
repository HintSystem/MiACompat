package dev.hintsystem.miacompat.server.mythic.drop;

import dev.hintsystem.miacompat.server.mythic.MythicParser;

public record ExperienceDrop(MythicParser.IntRange amount) implements DropEntry {}
