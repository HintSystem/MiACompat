package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;
import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import xaero.common.minimap.waypoints.Waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Waypoint.class, remap = false)
public class WaypointMixin {
    @Shadow private int x;
    @Shadow private int y;

    @Inject(method = "getX()I", at = @At("RETURN"), cancellable = true)
    private void modifyGetX(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(adjustX());
    }

    @Inject(method = "getX(D)I", at = @At("RETURN"), cancellable = true)
    private void modifyGetXWithDim(double dimDiv, CallbackInfoReturnable<Integer> cir) {
        int modifiedX = adjustX();
        int newResult = (dimDiv == 1.0) ? modifiedX : (int)Math.floor((double) modifiedX / dimDiv);

        cir.setReturnValue(newResult);
    }

    @Inject(method = "getY", at = @At("RETURN"), cancellable = true)
    private void modifyGetY(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(adjustY());
    }

    @Unique
    private int adjustX() {
        if (!SupportXaerosMinimap.isInWorldRenderer()) { return this.x; }
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();

        return (int) MiaDeeperWorld.relativizeWrapped(
            new Vec3(cameraPos.x, 0, 0), new Vec3(this.x, 0, 0)
        ).x();
    }

    @Unique
    private int adjustY() {
        if (!SupportXaerosMinimap.isInWorldRenderer()) { return this.y; }
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();

        return (int) MiaDeeperWorld.relativizeWrapped(
            new Vec3(cameraPos.x, cameraPos.y, 0), new Vec3(this.x, this.y, 0)
        ).y();
    }
}
