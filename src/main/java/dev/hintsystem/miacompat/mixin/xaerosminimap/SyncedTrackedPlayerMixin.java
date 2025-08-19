package dev.hintsystem.miacompat.mixin.xaerosminimap;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;
import dev.hintsystem.miacompat.utils.MiaWorldCoordinates;

import xaero.common.server.radar.tracker.SyncedTrackedPlayer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SyncedTrackedPlayer.class, remap = false)
public class SyncedTrackedPlayerMixin {
    @Shadow private double x;
    @Shadow private double y;

    @Inject(method = "getX()D", at = @At("RETURN"), cancellable = true)
    private void modifyGetX(CallbackInfoReturnable<Double> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || player == null) { return; }

        cir.setReturnValue(MiaWorldCoordinates.relativizeWrapped(
            new Vec3d(player.getX(), 0, 0), new Vec3d(this.x, 0, 0)
        ).getX());
    }
    @Inject(method = "getY()D", at = @At("RETURN"), cancellable = true)
    private void modifyGetY(CallbackInfoReturnable<Double> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || player == null) { return; }

        cir.setReturnValue(MiaWorldCoordinates.relativizeWrapped(
            new Vec3d(player.getX(), player.getY(), 0), new Vec3d(this.x, this.y, 0)
        ).getX());
    }
}
