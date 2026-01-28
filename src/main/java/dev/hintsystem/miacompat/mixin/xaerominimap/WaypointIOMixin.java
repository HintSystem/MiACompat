package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;

import xaero.hud.minimap.waypoint.io.WaypointIO;
import xaero.hud.minimap.world.MinimapWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.OutputStreamWriter;

@Mixin(value = WaypointIO.class, remap = false)
public class WaypointIOMixin {
    @Inject(method = "saveWaypoints", at = @At("HEAD"))
    public void onSaveWaypoints(MinimapWorld world, OutputStreamWriter output, CallbackInfo ci) {
        SupportXaerosMinimap.setInWorldRenderer(false); // Prevent saving visual waypoint modifications due to crashes
    }
}
