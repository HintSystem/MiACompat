package dev.hintsystem.miacompat.mods;

public class SupportXaerosMinimap {
    private static final ThreadLocal<Boolean> IN_WORLD_RENDERER = ThreadLocal.withInitial(() -> false);

    public static void setInWorldRenderer(boolean inRenderer) {
        IN_WORLD_RENDERER.set(inRenderer);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isInWorldRenderer() {
        return IN_WORLD_RENDERER.get();
    }
}
