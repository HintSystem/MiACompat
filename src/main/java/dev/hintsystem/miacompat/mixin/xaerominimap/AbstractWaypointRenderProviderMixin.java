package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.mods.xaerominimap.LayerAdjustedWaypoint;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.waypoint.render.AbstractWaypointRenderContext;
import xaero.hud.minimap.waypoint.render.AbstractWaypointRenderProvider;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = AbstractWaypointRenderProvider.class, remap = false)
public abstract class AbstractWaypointRenderProviderMixin<C extends AbstractWaypointRenderContext> {
    @Unique
    private final Map<Waypoint, LayerAdjustedWaypoint> waypointCache = new WeakHashMap<>(); // Waypoints must be cached, otherwise highlighting a waypoint will not work

    @ModifyReturnValue(
        method = "getNext(Lxaero/hud/minimap/element/render/MinimapElementRenderLocation;Lxaero/hud/minimap/waypoint/render/AbstractWaypointRenderContext;)Lxaero/common/minimap/waypoints/Waypoint;",
        at = @At("RETURN")
    )
    @SuppressWarnings("ConstantConditions")
    private Waypoint wrapGetNext(Waypoint w, MinimapElementRenderLocation location, C context) {
        if (!((Object) this instanceof WaypointWorldRenderProvider)) return w;

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        LayerAdjustedWaypoint layerWaypoint = waypointCache.get(w);
        if (layerWaypoint == null) {
            layerWaypoint = LayerAdjustedWaypoint.of(w, cameraPos);
            waypointCache.put(w, layerWaypoint);
        } else {
            layerWaypoint.update(cameraPos);
        }

        return layerWaypoint;
    }
}
