package dev.hintsystem.miacompat.mods;

import dev.hintsystem.miacompat.BonfireTracker;
import dev.hintsystem.miacompat.MiACompat;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.lib.client.graphics.XaeroRenderType;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;

import java.util.OptionalDouble;
import java.util.function.Supplier;

public class SupportXaeroMinimap {
    private static final Identifier BONFIRE_ICON_TEXTURE = MiACompat.id("textures/gui/bonfire_icon.png");

    private static final Supplier<GpuSampler> NEAREST_NO_MIPMAPS = () -> RenderSystem.getDevice().createSampler(
        AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
        FilterMode.NEAREST, FilterMode.NEAREST,
        1, OptionalDouble.empty()
    );

    public static final RenderType GUI_BONFIRE =
        XaeroRenderType.createRenderType(
            "xaero_gui_miacompat",
            RenderSetup.builder(XaeroRenderType.RP_POSITION_COLOR_TEX_TRANSLUCENT)
                .bufferSize(786432)
                .withTexture("Sampler0", BONFIRE_ICON_TEXTURE, NEAREST_NO_MIPMAPS)
                .setOutputTarget(OutputTarget.MAIN_TARGET)
        );

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
