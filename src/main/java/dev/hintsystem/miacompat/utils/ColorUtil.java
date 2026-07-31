package dev.hintsystem.miacompat.utils;

import net.minecraft.util.ARGB;

import java.awt.*;

public class ColorUtil {
    public static float getLuma(int argb) {
        return getLuma(ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb));
    }

    public static float getLuma(int r, int g, int b) {
        return (0.2126f * r) + (0.7152f * g) + (0.0722f * b);
    }

    public static int adjustHSB(int argb, float saturationScale, float brightnessScale) {
        float[] hsb = ARGBtoHSB(argb);
        hsb[1] = Math.clamp(hsb[1] * saturationScale, 0f, 1f);
        hsb[2] = Math.clamp(hsb[2] * brightnessScale, 0f, 1f);

        return HSBtoARGB(ARGB.alpha(argb), hsb);
    }

    public static float[] ARGBtoHSB(int argb) {
        return Color.RGBtoHSB(
            ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb),
            null
        );
    }

    public static int HSBtoARGB(int alpha, float[] hsb) {
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);

        return ARGB.color(alpha,
            (rgb >> 16) & 0xFF,
            (rgb >> 8) & 0xFF,
            rgb & 0xFF
        );
    }
}
