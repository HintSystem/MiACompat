package dev.hintsystem.miacompat.mods;

import dev.hintsystem.miacompat.BonfireTracker;
import dev.hintsystem.miacompat.MiACompat;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;

public class SupportXaerosMinimap {
    public static final Waypoint bonfireWaypoint = new Waypoint(0, 0, 0, "Bonfire", "B", WaypointColor.GOLD);

    private static final ThreadLocal<Boolean> IN_WORLD_RENDERER = ThreadLocal.withInitial(() -> false);

    public static void setInWorldRenderer(boolean inRenderer) {
        IN_WORLD_RENDERER.set(inRenderer);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isInWorldRenderer() {
        return IN_WORLD_RENDERER.get();
    }

    public static boolean shouldShowBonfireWaypoint() {
        return MiACompat.config.showBonfireWaypoint
            && MiACompat.isMiAServer()
            && BonfireTracker.bonfireData.isBonfireSet;
    }

    public static void updateBonfireWaypoint() {
        var bonfire = BonfireTracker.bonfireData;

        bonfireWaypoint.setX(bonfire.x);
        bonfireWaypoint.setY(bonfire.y);
        bonfireWaypoint.setZ(bonfire.z);
    }
}
