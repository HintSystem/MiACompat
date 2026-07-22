package dev.hintsystem.miacompat.server.config.geary.item;

import net.minecraft.network.chat.Component;

public enum RelicGrade {
    I(Component.literal("Grade I").withColor(0xFFE5663D)),
    II(Component.literal("Grade II").withColor(0xFF43583C)),
    III(Component.literal("Grade III").withColor(0xFFD7C9B3)),
    IV(Component.literal("Grade IV").withColor(0xFFBB9672));

    public final Component displayName;

    RelicGrade(Component displayName) {
        this.displayName = displayName;
    }
}
