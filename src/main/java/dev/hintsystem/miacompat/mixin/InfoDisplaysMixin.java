package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.utils.MiaWorldCoordinates;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.InfoDisplays;
import xaero.hud.minimap.info.codec.InfoDisplayCommonStateCodecs;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InfoDisplays.class, remap = false)
public class InfoDisplaysMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructed(CallbackInfo ci) {
        InfoDisplays self = (InfoDisplays) (Object) this;

        self.getManager().add(
            InfoDisplay.Builder.<Boolean>begin()
                .setId("mia_coords").setName(Text.literal("MiA Coordinates"))
                .setDefaultState(true).setCodec(InfoDisplayCommonStateCodecs.BOOLEAN).setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
                .setCompiler((displayInfo, compiler, session, availableWidth, playerPos) -> {
                    if (displayInfo.getState()) {
                        BlockPos MiAPos = MiaWorldCoordinates.unwrap(playerPos);
                        String coords = MiAPos.getX() + ", " + MiAPos.getY() + ", " + MiAPos.getZ();

                        if (MinecraftClient.getInstance().textRenderer.getWidth(coords) >= availableWidth) {
                            compiler.addLine(MiAPos.getX() + ", " + MiAPos.getZ());
                            compiler.addLine("" + MiAPos.getY());
                        } else {
                            compiler.addLine(coords);
                        }
                    }
                }).build()
        );
    }
}
