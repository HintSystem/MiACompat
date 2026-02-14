package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.BonfireTracker;

import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerMixin {
    @Inject(method = "handleSystemMessage", at = @At("HEAD"))
    public void miacompat$onGameMessage(Component message, boolean overlay, CallbackInfo ci) {
        if (overlay) return;

        BonfireTracker.onServerMessage(message);
    }
}
