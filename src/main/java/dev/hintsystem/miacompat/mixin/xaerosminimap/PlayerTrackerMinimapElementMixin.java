package dev.hintsystem.miacompat.mixin.xaerosminimap;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;
import dev.hintsystem.miacompat.utils.MiaWorldCoordinates;

import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;
import xaero.hud.minimap.player.tracker.system.IRenderedPlayerTracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerTrackerMinimapElement.class, remap = false)
public class PlayerTrackerMinimapElementMixin<P> {
    @Shadow
    private P player;
    @Shadow
    private IRenderedPlayerTracker<P> system;

    @Inject(method = "getX()D", at = @At("RETURN"), cancellable = true)
    private void modifyGetX(CallbackInfoReturnable<Double> cir) {
        ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || clientPlayer == null) { return; }

        double trackedX = system.getReader().getX(player);

        cir.setReturnValue(MiaWorldCoordinates.relativizeWrapped(
            new Vec3d(clientPlayer.getX(), 0, 0),
            new Vec3d(trackedX, 0, 0)
        ).getX());
    }

    @Inject(method = "getY()D", at = @At("RETURN"), cancellable = true)
    private void modifyGetY(CallbackInfoReturnable<Double> cir) {
        ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || clientPlayer == null) { return; }

        double trackedX = system.getReader().getX(player);
        double trackedY = system.getReader().getY(player);

        cir.setReturnValue(MiaWorldCoordinates.relativizeWrapped(
            new Vec3d(clientPlayer.getX(), clientPlayer.getY(), 0),
            new Vec3d(trackedX, trackedY, 0)
        ).getY());
    }
}
