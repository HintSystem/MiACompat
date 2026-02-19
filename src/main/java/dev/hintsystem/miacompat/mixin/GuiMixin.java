package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.InventoryTracker;
import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;

import com.llamalad7.mixinextras.sugar.Cancellable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(Gui.class)
public class GuiMixin {
    @ModifyVariable(
        method = "setOverlayMessage",
        at = @At("HEAD"),
        argsOnly = true
    )
    public Component miacompat$onOverlayMessage(Component message, @Cancellable CallbackInfo ci) {
        Component editedText = MiACompat.ghostSeekTracker.onActionbarMessage(message);
        if (editedText == null) editedText = InventoryTracker.onActionbarMessage(message);

        if (Objects.equals(editedText, Component.empty())) { ci.cancel(); }

        return editedText != null ? editedText : message;
    }
}
