package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.client.CooldownTracker;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "swing", at = @At("HEAD"))
    public void miacompat$onSwing(InteractionHand hand, CallbackInfo ci) {
        CooldownTracker.onItemLeftClick(
            ((LocalPlayer)(Object) this).getItemInHand(hand)
        );
    }
}
