package dev.hintsystem.miacompat.mods.xaerominimap;

import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.WaypointVisibilityType;

import net.minecraft.world.phys.Vec3;

public class LayerAdjustedWaypoint extends Waypoint {
    private final Waypoint original;

    public LayerAdjustedWaypoint(Waypoint waypoint) {
        super(
            waypoint.getX(), waypoint.getY(), waypoint.getZ(),
            waypoint.getName(), waypoint.getInitials(), waypoint.getWaypointColor(), waypoint.getPurpose(),
            waypoint.isTemporary(), true
        );

        this.original = waypoint;
        updateForCamera();
    }

    public void updateForCamera() {
        Vec3 relativePosition = MiaDeeperWorld.relativeToCamera(
            new Vec3(original.getX(), original.getY(), original.getZ())
        );

        this.setX((int) relativePosition.x);
        this.setY((int) relativePosition.y);
        this.setZ(original.getZ());
    }

    @Override
    public int getYaw() { return original.getYaw(); }

    @Override
    public String getName() { return original.getName(); }

    @Override
    public String getLocalizedName() { return original.getLocalizedName(); }

    @Override
    public String getInitials() { return original.getInitials(); }

    @Override
    public WaypointColor getWaypointColor() { return original.getWaypointColor(); }

    @Override
    public WaypointVisibilityType getVisibility() { return original.getVisibility(); }

    @Override
    public Boolean getDisabled() { return original.getDisabled(); }

    @Override
    public WaypointPurpose getPurpose() { return original.getPurpose(); }

    @Override
    public boolean isRotation() { return original.isRotation(); }

    @Override
    public boolean isTemporary() { return original.isTemporary(); }

    @Override
    public long getCreatedAt() { return original.getCreatedAt(); }
}
