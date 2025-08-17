package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;
import dev.hintsystem.miacompat.utils.MiaWorldCoordinates;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import xaero.common.minimap.waypoints.Waypoint;

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
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || player == null) { return this.x; }

        return (int)MiaWorldCoordinates.relativizeWrapped(
            new Vec3d(player.getX(), 0, 0), new Vec3d(this.x, 0, 0)
        ).getX();
    }

    @Unique
    private int adjustY() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || player == null) { return this.y; }

        return (int)MiaWorldCoordinates.relativizeWrapped(
            new Vec3d(player.getX(), player.getY(), 0), new Vec3d(this.x, this.y, 0)
        ).getY();
    }
}
