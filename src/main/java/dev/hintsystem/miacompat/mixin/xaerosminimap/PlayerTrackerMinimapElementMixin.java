package dev.hintsystem.miacompat.mixin.xaerosminimap;

import dev.hintsystem.miacompat.mods.SupportXaerosMinimap;
import dev.hintsystem.miacompat.utils.MiaWorldCoordinates;

import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;
import xaero.hud.minimap.player.tracker.system.IRenderedPlayerTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

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
        LocalPlayer clientPlayer = Minecraft.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || clientPlayer == null) { return; }

        double trackedX = system.getReader().getX(player);

        cir.setReturnValue(MiaWorldCoordinates.relativizeWrapped(
            new Vec3(clientPlayer.getX(), 0, 0),
            new Vec3(trackedX, 0, 0)
        ).x());
    }

    @Inject(method = "getY()D", at = @At("RETURN"), cancellable = true)
    private void modifyGetY(CallbackInfoReturnable<Double> cir) {
        LocalPlayer clientPlayer = Minecraft.getInstance().player;
        if (!SupportXaerosMinimap.isInWorldRenderer() || clientPlayer == null) { return; }

        double trackedX = system.getReader().getX(player);
        double trackedY = system.getReader().getY(player);

        cir.setReturnValue(MiaWorldCoordinates.relativizeWrapped(
            new Vec3(clientPlayer.getX(), clientPlayer.getY(), 0),
            new Vec3(trackedX, trackedY, 0)
        ).y());
    }
}
