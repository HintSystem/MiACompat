package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.mods.SupportXaeroMinimap;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointCollector;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WaypointCollector.class, remap = false)
public class WaypointCollectorMixin {

    @Inject(method = "collect", at = @At("HEAD"))
    public void miacompat$collect(List<Waypoint> destination, CallbackInfo ci) {
        if (!SupportXaeroMinimap.shouldShowBonfireWaypoint()) return;

        SupportXaeroMinimap.updateBonfireWaypoint();
        destination.add(SupportXaeroMinimap.bonfireWaypoint);
    }
}
