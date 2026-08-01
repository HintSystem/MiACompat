package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.CooldownTracker;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

    @ModifyExpressionValue(
        method = "renderCameraOverlays",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/equipment/Equippable;cameraOverlay()Ljava/util/Optional;"
        )
    )
    private Optional<Identifier> miaCompat$replaceCameraOverlay(Optional<Identifier> original) {
        if (
            MiACompat.config.fixGhostSeekCameraOverlay
            && original.isPresent()
            && original.get().equals(MiACompat.idMiA("misc/ghost_seek_overlay"))
        ) {
            return Optional.of(MiACompat.idMiA("gear/utility/ghost_seek_overlay"));
        }

        return original;
    }
}
