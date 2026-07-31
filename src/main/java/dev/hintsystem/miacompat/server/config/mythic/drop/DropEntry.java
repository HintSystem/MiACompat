package dev.hintsystem.miacompat.server.config.mythic.drop;

import dev.hintsystem.miacompat.server.config.mythic.MythicParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.*;

import org.jetbrains.annotations.Nullable;

public sealed interface DropEntry permits ItemDrop, RelicDrop, DropTableReference, ExperienceDrop {
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

        switch (dropName.toLowerCase(Locale.ROOT)) {
            case "experience", "exp", "xp" -> {
                return new ExperienceDrop(MythicParser.IntRange.parse(container.get(1)));
            }
            case "geary" -> {
                Identifier itemId = Identifier.parse(container.get(1));

                return parseItemDrop(
                    container.subList(1, container.size()),
                    itemId, Map.of()
                );
            }
            default -> {
                if (container.size() == 1)
                    return new DropTableReference(dropName);

                MythicParser.Invocation invocation = MythicParser.Invocation.parse(dropName);

                Identifier itemId = Identifier.withDefaultNamespace(
                    invocation.name().toLowerCase(Locale.ROOT)
                );

                if (!BuiltInRegistries.ITEM.containsKey(itemId))
                    throw new IllegalArgumentException("Invalid drop entry: " + drop);

                return parseItemDrop(
                    container, itemId, invocation.arguments()
                );
            }
        }
    }

    private static ItemDrop parseItemDrop(
        List<String> container,
        Identifier itemId, Map<String, String> arguments
    ) {
        double chance = 1;
        if (container.size() > 2)
            chance = Double.parseDouble(container.get(2));

        return new ItemDrop(
            itemId, arguments,
            MythicParser.IntRange.parse(container.get(1)), chance,
            parseDropFlags(container)
        );
    }

    private static EnumSet<ItemDrop.DropFlag> parseDropFlags(List<String> container) {
        EnumSet<ItemDrop.DropFlag> flags = EnumSet.noneOf(ItemDrop.DropFlag.class);

        for (int i = 3; i < container.size(); i++) {
            switch (container.get(i).toLowerCase(Locale.ROOT)) {
                case "nolooting" -> flags.add(ItemDrop.DropFlag.NO_LOOTING);
                default ->
                    throw new IllegalArgumentException("Unknown drop flag '" + container.get(i) + "'");
            }
        }

        return flags;
    }
}
