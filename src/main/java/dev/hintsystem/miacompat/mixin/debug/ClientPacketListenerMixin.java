package dev.hintsystem.miacompat.mixin.debug;

import dev.hintsystem.miacompat.debug.PacketDebug;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void handleAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        PacketDebug.handleAddEntityPacket(packet);
    }

    @Inject(
        method = "handleRemoveEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            shift = At.Shift.AFTER
        )
    )
    private void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        PacketDebug.handleRemoveEntitiesPacket(packet);
    }
}
