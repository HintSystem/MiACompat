package dev.hintsystem.miacompat.server.config.mythic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MythicParser {

    public record IntRange(int min, int max) {
        public static IntRange parse(String text) {
            if (text.contains("-")) {
                String[] split = text.split("-", 2);
                return new IntRange(
                    Integer.parseInt(split[0]),
                    Integer.parseInt(split[1])
                );
            }

            int value = Integer.parseInt(text);
            return new IntRange(value, value);
        }
    }

    /**
     * Represents a MythicMobs invocation of the form:
     *
     * <pre>
     * name{key=value;key2=value2}
     * </pre>
     *
     * For example:
     *
     * <pre>
     * WITHER_ROSE{name="<#36454F>Charred Flower";lore="<gold>Found during the event."}
     * </pre>
     *
     * is parsed into the invocation name ({@code WITHER_ROSE}) and a map of
     * argument key/value pairs. Argument values are preserved exactly as written
     */
    public record Invocation(
        String name,
        Map<String, String> arguments
    ) {
        public static Invocation parse(String text) {
            int brace = text.indexOf('{');

            if (brace == -1)
                return new Invocation(text, Map.of());

            if (!text.endsWith("}"))
                throw new IllegalArgumentException("Malformed invocation: " + text);

            String name = text.substring(0, brace);
            String body = text.substring(brace + 1, text.length() - 1);

            return new Invocation(name, parseArguments(body));
        }

        private static Map<String, String> parseArguments(String body) {
            if (body.isBlank())
                return Map.of();

            Map<String, String> arguments = new LinkedHashMap<>();

            for (String argument : tokenize(body, ';')) {
                int equals = argument.indexOf('=');

                if (equals == -1) {
                    arguments.put(argument.trim(), "");
                    continue;
                }

                String key = argument.substring(0, equals).trim();
                String value = argument.substring(equals + 1).trim();

                arguments.put(key, value);
            }

            return arguments;
        }
    }

    /**
     * Splits a string on the given delimiter while ignoring delimiters contained
     * inside nested {@code {}}, {@code []}, {@code ()}, or quoted strings
     *
     * <p>This is used to parse MythicMobs' syntax, where delimiters such as spaces
     * and semicolons may appear inside argument blocks without terminating the
     * current token</p>
     *
     * <p>For example, tokenizing on spaces preserves:</p>
     *
     * <pre>
     * WITHER_ROSE{name="Charred Flower"} 1 0.05
     * </pre>
     *
     * as:
     *
     * <pre>
     * [
     *   "WITHER_ROSE{name=\"Charred Flower\"}",
     *   "1",
     *   "0.05"
     * ]
     * </pre>
     *
     * @param line the string to split
     * @param delimiter the delimiter to split on
     * @return the top-level tokens in the order they appear
     */
    public static List<String> tokenize(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        int braceDepth = 0;
        int bracketDepth = 0;
        int parenDepth = 0;

        char quote = 0;
        boolean escaped = false;

        for (char c : line.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (quote != 0) {
                current.append(c);

                if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }

                continue;
            }

            switch (c) {
                case '"', '\'' -> {
                    quote = c;
                    current.append(c);
                }

                case '{' -> {
                    braceDepth++;
                    current.append(c);
                }

                case '}' -> {
                    braceDepth--;
                    current.append(c);
                }

                case '[' -> {
                    bracketDepth++;
                    current.append(c);
                }

                case ']' -> {
                    bracketDepth--;
                    current.append(c);
                }

                case '(' -> {
                    parenDepth++;
                    current.append(c);
                }

                case ')' -> {
                    parenDepth--;
                    current.append(c);
                }

                default -> {
                    if (c == delimiter
                        && braceDepth == 0
                        && bracketDepth == 0
                        && parenDepth == 0) {

                        tokens.add(current.toString());
                        current.setLength(0);
                    } else {
                        current.append(c);
                    }
                }
            }
        }

        tokens.add(current.toString());
        return tokens;
    }
}