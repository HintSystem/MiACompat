package dev.hintsystem.miacompat.mixin.xaeroworldmap;

import dev.hintsystem.miacompat.mods.SupportXaeroMinimap;

import xaero.map.mods.gui.Waypoint;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = xaero.map.mods.SupportXaeroMinimap.class, remap = false)
public abstract class SupportXaeroMinimapMixin {

    @Shadow
    public abstract Waypoint convertWaypoint(xaero.common.minimap.waypoints.Waypoint w, boolean editable, String setName, double dimDiv);

    @Inject(method = "getWaypoints", at = @At("RETURN"), cancellable = true)
    public void getWaypoints(CallbackInfoReturnable<ArrayList<Waypoint>> cir) {
        if (!SupportXaeroMinimap.shouldShowBonfireWaypoint()) return;

        SupportXaeroMinimap.updateBonfireWaypoint();

        var originalArray = cir.getReturnValue();

        var newArray = new ArrayList<>(originalArray);
        newArray.add(convertWaypoint(SupportXaeroMinimap.bonfireWaypoint, false, "", 1.0d));

        cir.setReturnValue(newArray);
    }
}
