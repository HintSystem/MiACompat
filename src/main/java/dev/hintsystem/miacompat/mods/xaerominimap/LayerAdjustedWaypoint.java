package dev.hintsystem.miacompat.mods.xaerominimap;

import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import xaero.common.minimap.waypoints.Waypoint;

import net.minecraft.world.phys.Vec3;

public class LayerAdjustedWaypoint extends Waypoint {
    private final Waypoint original;

    private LayerAdjustedWaypoint(Vec3 pos, Waypoint waypoint) {
        super(
            (int) pos.x(), (int) pos.y(), (int) pos.z(),
            waypoint.getName(), waypoint.getInitials(), waypoint.getWaypointColor(), waypoint.getPurpose(),
            waypoint.isTemporary(), true
        );

        this.original = waypoint;
    }

    public void update(Vec3 cameraPos) {
        Vec3 relativePosition = MiaDeeperWorld.relativizeWrapped(
            cameraPos, new Vec3(original.getX(), original.getY(), original.getZ())
        );

        this.setX((int) relativePosition.x);
        this.setY((int) relativePosition.y);
    }

    public static LayerAdjustedWaypoint of(Waypoint waypoint, Vec3 cameraPos) {
        Vec3 relativePosition = MiaDeeperWorld.relativizeWrapped(
            cameraPos, new Vec3(waypoint.getX(), waypoint.getY(), waypoint.getZ())
        );

        return new LayerAdjustedWaypoint(relativePosition, waypoint);
    }
}
