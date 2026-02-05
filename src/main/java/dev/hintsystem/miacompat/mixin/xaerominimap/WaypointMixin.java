package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;
import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import xaero.common.minimap.waypoints.Waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        LocalPlayer player = Minecraft.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || player == null) { return this.x; }

        return (int) MiaDeeperWorld.relativizeWrapped(
            new Vec3(player.getX(), 0, 0), new Vec3(this.x, 0, 0)
        ).x();
    }

    @Unique
    private int adjustY() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || player == null) { return this.y; }

        return (int) MiaDeeperWorld.relativizeWrapped(
            new Vec3(player.getX(), player.getY(), 0), new Vec3(this.x, this.y, 0)
        ).y();
    }
}
