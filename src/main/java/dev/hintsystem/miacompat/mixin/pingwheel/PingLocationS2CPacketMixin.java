package dev.hintsystem.miacompat.mixin.pingwheel;

import dev.hintsystem.miacompat.utils.MiaWorldCoordinates;

import nx.pingwheel.common.network.PingLocationS2CPacket;

import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PingLocationS2CPacket.class, remap = false)
public class PingLocationS2CPacketMixin {

    @Inject(method = "pos", at = @At("RETURN"), cancellable = true)
    private void modifyPos(CallbackInfoReturnable<Vec3d> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) { return; }

        Vec3d original = cir.getReturnValue();
        cir.setReturnValue(MiaWorldCoordinates.relativizeWrapped(player.getPos(), original));
    }
}
