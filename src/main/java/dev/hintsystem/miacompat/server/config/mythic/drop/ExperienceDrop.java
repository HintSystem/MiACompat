package dev.hintsystem.miacompat.server.config.mythic.drop;

import dev.hintsystem.miacompat.server.config.mythic.MythicParser;

public record ExperienceDrop(MythicParser.IntRange amount) implements DropEntry {}
