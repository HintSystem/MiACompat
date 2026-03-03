package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.client.CooldownTracker;
import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;

import com.llamalad7.mixinextras.sugar.Cancellable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @ModifyVariable(
        method = "setOverlayMessage",
        at = @At("HEAD"),
        argsOnly = true
    )
    public Component miacompat$onOverlayMessage(Component message, @Cancellable CallbackInfo ci) {
        if (!CooldownTracker.allowActionBarMessage(message)) { ci.cancel(); }

        return MiACompat.ghostSeekTracker.modifyActionbarMessage(message);
    }
}
