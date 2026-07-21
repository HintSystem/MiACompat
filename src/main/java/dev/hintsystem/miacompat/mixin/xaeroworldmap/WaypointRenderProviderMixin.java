package dev.hintsystem.miacompat.mixin.xaeroworldmap;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.GhostSeekTracker;
import dev.hintsystem.miacompat.mods.xaeroworldmap.CustomColorWaypoint;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.mods.gui.WaypointRenderContext;
import xaero.map.mods.gui.WaypointRenderProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WaypointRenderProvider.class, remap = false)
public class WaypointRenderProviderMixin {
    @Shadow
    private Iterator<xaero.map.mods.gui.Waypoint> iterator;

    @Inject(
        method = "begin(Lxaero/map/element/render/ElementRenderLocation;Lxaero/map/mods/gui/WaypointRenderContext;)V",
        at = @At("TAIL")
    )
    private void addBreadcrumbs(ElementRenderLocation location, WaypointRenderContext context, CallbackInfo ci) {
        if (!MiACompat.config.showBreadcrumbsOnMap || !MiACompat.ghostSeekTracker.breadcrumbsVisible()) return;

        List<GhostSeekTracker.Measurement> breadcrumbs = MiACompat.ghostSeekTracker.getMeasurements();
        if (breadcrumbs.isEmpty()) return;

        List<xaero.map.mods.gui.Waypoint> breadcrumbWaypoints = new ArrayList<>();

        for (GhostSeekTracker.Measurement b : breadcrumbs) {
            breadcrumbWaypoints.add(new CustomColorWaypoint(
                new Waypoint(
                    (int) b.position.x, (int) b.position.y, (int) b.position.z,
                    "Breadcrumb", "B", WaypointColor.BLACK
                ),
                b.getColor(MiACompat.ghostSeekTracker.getMaxRange()), false, "", 1
            ));
        }

        if (this.iterator != null) {
            this.iterator.forEachRemaining(breadcrumbWaypoints::add);
        }
        this.iterator = breadcrumbWaypoints.iterator();
    }
}
