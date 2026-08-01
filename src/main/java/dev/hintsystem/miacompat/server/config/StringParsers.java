package dev.hintsystem.miacompat.server.config;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.time.Duration;

public class StringParsers {
    public static Identifier parseIdentifier(String value) {
        Identifier id = Identifier.tryParse(value);
        if (id == null)
            throw new IllegalArgumentException("Not a valid identifier: " + value);

        return id;
    }

    public static BlockPos parseBlockPos(String value) {
        String[] p = value.split(",");

        if (p.length != 3)
            throw new IllegalArgumentException("Not a valid block position: " + value);

        return new BlockPos(
            Integer.parseInt(p[0].trim()),
            Integer.parseInt(p[1].trim()),
            Integer.parseInt(p[2].trim())
        );
    }

    public static Duration parseDuration(String value) {
        int splitAt = 0;
        while (splitAt < value.length() && !Character.isLetter(value.charAt(splitAt))) {
            splitAt++;
        }

        if (splitAt == 0 || splitAt == value.length())
            throw new IllegalArgumentException("Not a valid duration: " + value);

        double duration = Double.parseDouble(value.substring(0, splitAt));
        String unit = value.substring(splitAt);

        final long TICK = 50;
        final long SECOND = 1_000;
        final long MINUTE = SECOND * 60;
        final long HOUR = MINUTE * 60;
        final long DAY = HOUR * 24;
        final long WEEK = DAY * 7;
        final long MONTH = DAY * 31;

        return Duration.ofMillis((long) switch (unit) {
            case "ms" -> duration;
            case "t" -> duration * TICK;
            case "s" -> duration * SECOND;
            case "m" -> duration * MINUTE;
            case "h" -> duration * HOUR;
            case "d" -> duration * DAY;
            case "w" -> duration * WEEK;
            case "mo" -> duration * MONTH;
            default -> throw new IllegalArgumentException("Unknown duration unit '" + unit + "' in: " + duration);
        });
    }
}
