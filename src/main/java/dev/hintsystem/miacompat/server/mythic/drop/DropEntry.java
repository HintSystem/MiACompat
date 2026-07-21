package dev.hintsystem.miacompat.server.mythic.drop;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.mythic.MythicParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.*;

import org.jetbrains.annotations.Nullable;

public sealed interface DropEntry permits ItemDrop, DropTableReference, ExperienceDrop {
    static List<DropEntry> parseList(@Nullable List<String> drops) {
        if (drops == null) return List.of();

        List<DropEntry> dropEntries = new ArrayList<>();
        for (String line : drops) {
            if (line.isBlank()) continue;
            dropEntries.add(DropEntry.parse(line));
        }

        return dropEntries;
    }

    static DropEntry parse(String drop) {
        List<String> container = MythicParser.tokenize(drop, ' ');
        String dropName = container.getFirst();

        if (container.size() == 1)
            return new DropTableReference(dropName);

        switch (dropName.toLowerCase(Locale.ROOT)) {
            case "geary" -> {
                return parseGearyDrop(container);
            }
            case "experience", "exp", "xp" -> {
                return new ExperienceDrop(MythicParser.IntRange.parse(container.get(1)));
            }
            default -> {
                MythicParser.Invocation invocation = MythicParser.Invocation.parse(dropName);

                Identifier itemId = Identifier.withDefaultNamespace(
                    invocation.name().toLowerCase(Locale.ROOT)
                );

                if (BuiltInRegistries.ITEM.get(itemId).isEmpty())
                    throw new IllegalArgumentException("Invalid drop entry: " + drop);

                return new ItemDrop(
                    itemId, invocation.arguments(),
                    MythicParser.IntRange.parse(container.get(1)), Double.parseDouble(container.get(2)),
                    parseDropFlags(container, 3)
                );
            }
        }
    }

    private static ItemDrop parseGearyDrop(List<String> container) {
        return new ItemDrop(
            Identifier.parse(container.get(1)), Map.of(),
            MythicParser.IntRange.parse(container.get(2)), Double.parseDouble(container.get(3)),
            parseDropFlags(container, 4)
        );
    }

    private static EnumSet<ItemDrop.DropFlag> parseDropFlags(List<String> container, int flagsStartIndex) {
        EnumSet<ItemDrop.DropFlag> flags = EnumSet.noneOf(ItemDrop.DropFlag.class);

        for (int i = flagsStartIndex; i < container.size(); i++) {
            switch (container.get(i).toLowerCase(Locale.ROOT)) {
                case "nolooting" -> flags.add(ItemDrop.DropFlag.NO_LOOTING);
                default -> MiACompat.LOGGER.warn("Unknown drop flag '{}'", container.get(i));
            }
        }

        return flags;
    }
}
