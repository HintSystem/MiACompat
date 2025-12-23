package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameHud.class)
public class HudMixin {
    @ModifyVariable(
        method = "setOverlayMessage",
        at = @At("HEAD"),
        argsOnly = true
    )
    public Text onOverlayMessage(Text message) {
        Text newActionBarText = MiACompat.ghostSeekTracker.onActionbarMessage(message);
        return newActionBarText != null ? newActionBarText : message;
    }
}
