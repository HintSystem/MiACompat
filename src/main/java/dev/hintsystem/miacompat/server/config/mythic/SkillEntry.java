package dev.hintsystem.miacompat.server.config.mythic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public record SkillEntry(
    MythicParser.Invocation mechanic,
    @Nullable String targeter,
    @Nullable String trigger,
    @Nullable Double chance
) {

    public boolean isCustomSkill() {
        return mechanic.name().equalsIgnoreCase("skill");
    }

    @Nullable
    public String customSkillName() {
        return isCustomSkill() ? mechanic.arguments().get("s") : null;
    }

    public static List<dev.hintsystem.miacompat.server.config.mythic.SkillEntry> parseList(@Nullable List<String> skills) {
        if (skills == null) return List.of();

        List<dev.hintsystem.miacompat.server.config.mythic.SkillEntry> skillEntries = new ArrayList<>();
        for (String line : skills) {
            if (line.isBlank()) continue;
            skillEntries.add(dev.hintsystem.miacompat.server.config.mythic.SkillEntry.parse(line));
        }
        return skillEntries;
    }

    public static dev.hintsystem.miacompat.server.config.mythic.SkillEntry parse(String line) {
        List<String> tokens = MythicParser.tokenize(line, ' ');

        MythicParser.Invocation mechanic = MythicParser.Invocation.parse(tokens.getFirst());

        String targeter = null;
        String trigger = null;
        Double chance = null;

        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (token.startsWith("@")) {
                targeter = token;
            } else if (token.startsWith("~")) {
                trigger = token;
            } else {
                try {
                    chance = Double.parseDouble(token);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return new dev.hintsystem.miacompat.server.config.mythic.SkillEntry(mechanic, targeter, trigger, chance);
    }
}
