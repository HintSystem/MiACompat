package dev.hintsystem.miacompat.mixin.pingwheel;

import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import nx.pingwheel.common.network.PingLocationS2CPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PingLocationS2CPacket.class, remap = false)
public class PingLocationS2CPacketMixin {

    @Inject(method = "pos", at = @At("RETURN"), cancellable = true)
    private void modifyPos(CallbackInfoReturnable<Vec3> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) { return; }

        Vec3 original = cir.getReturnValue();
        cir.setReturnValue(MiaDeeperWorld.relativizeWrapped(player.position(), original));
    }
}
