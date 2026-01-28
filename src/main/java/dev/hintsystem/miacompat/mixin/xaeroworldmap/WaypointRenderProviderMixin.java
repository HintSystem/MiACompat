package dev.hintsystem.miacompat.mixin.xaeroworldmap;

import dev.hintsystem.miacompat.GhostSeekTracker;
import dev.hintsystem.miacompat.MiACompat;

import xaero.map.element.render.ElementRenderLocation;
import xaero.map.mods.gui.Waypoint;
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
    private Iterator<Waypoint> iterator;

    @Inject(
        method = "begin(Lxaero/map/element/render/ElementRenderLocation;Lxaero/map/mods/gui/WaypointRenderContext;)V",
        at = @At("TAIL")
    )
    private void addBreadcrumbs(ElementRenderLocation location, WaypointRenderContext context, CallbackInfo ci) {
        if (!MiACompat.config.showBreadcrumbsOnMap || !MiACompat.ghostSeekTracker.breadcrumbsVisible()) return;

        List<GhostSeekTracker.Measurement> breadcrumbs = MiACompat.ghostSeekTracker.getMeasurements();
        if (breadcrumbs.isEmpty()) return;

        List<Waypoint> breadcrumbWaypoints = new ArrayList<>();

        for (GhostSeekTracker.Measurement b : breadcrumbs) {
            breadcrumbWaypoints.add(new Waypoint(
                new Object(),
                (int) b.position.x, (int) b.position.y, (int) b.position.z,
                "Breadcrumb", "B", b.getColor(MiACompat.ghostSeekTracker.getMaxRange()),
                0, false, "", true, 1.0f
            ));
        }

        if (this.iterator != null) {
            this.iterator.forEachRemaining(breadcrumbWaypoints::add);
        }
        this.iterator = breadcrumbWaypoints.iterator();
    }
}
