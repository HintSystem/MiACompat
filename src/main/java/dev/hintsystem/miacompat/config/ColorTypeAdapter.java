package dev.hintsystem.miacompat.config;

import java.awt.*;
import java.io.IOException;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ColorTypeAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter out, Color value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        // Format as #RRGGBBAA or #RRGGBB if alpha is max
        String hex = String.format("#%02X%02X%02X", value.getRed(), value.getGreen(), value.getBlue());
        if (value.getAlpha() != 255) {
            hex += String.format("%02X", value.getAlpha());
        }

        out.value(hex);
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String hex = in.nextString();

        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        int r, g, b, a = 255;
        switch (hex.length()) {
            case 6 -> {
                // #RRGGBB
                r = Integer.parseInt(hex.substring(0, 2), 16);
                g = Integer.parseInt(hex.substring(2, 4), 16);
                b = Integer.parseInt(hex.substring(4, 6), 16);
            }
            case 8 -> {
                // #RRGGBBAA
                r = Integer.parseInt(hex.substring(0, 2), 16);
                g = Integer.parseInt(hex.substring(2, 4), 16);
                b = Integer.parseInt(hex.substring(4, 6), 16);
                a = Integer.parseInt(hex.substring(6, 8), 16);
            }
            default -> throw new JsonParseException("Invalid color: #" + hex);
        }

        return new Color(r, g, b, a);
    }
}
