package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.utils.MiaDeeperWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Final;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.InfoDisplayManager;
import xaero.hud.minimap.info.InfoDisplays;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;
import xaero.lib.common.config.option.value.io.serialization.BuiltInConfigValueIOCodecs;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InfoDisplays.class, remap = false)
public class InfoDisplaysMixin {
    @Final @Shadow
    private InfoDisplayManager manager;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/hud/minimap/info/BuiltInInfoDisplays;forEach(Ljava/util/function/Consumer;)V"
        )
    )
    private void onConstructed(CallbackInfo ci) {
        manager.add(
            InfoDisplay.Builder.<Boolean>begin()
                .setId("mia_layer").setName(Component.literal("MiA Layer"))
                .setDefaultState(true).setCodec(BuiltInConfigValueIOCodecs.BOOLEAN).setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
                .setCompiler((displayInfo, compiler, session, availableWidth, playerPos) -> {
                    if (displayInfo.getEffectiveState()) {
                        int section = MiaDeeperWorld.sectionFromX(playerPos.getX());
                        MiaDeeperWorld.LayerInfo layer = MiaDeeperWorld.LayerInfo.fromUnwrappedY(
                            MiaDeeperWorld.unwrapY(playerPos.getY(), section)
                        );

                        if (layer.ordinal() != 0) {
                            String layerShort = "L" + layer.ordinal() + "S" + layer.getSubSection(section);
                            compiler.addLine(
                                Component.literal(layerShort)
                                    .setStyle(Style.EMPTY.withColor(layer.subtitleColor != null ? layer.subtitleColor : layer.titleColor))
                            );
                        }

                        compiler.addLine(
                            Component.literal(layer.title)
                                .setStyle(Style.EMPTY.withColor(layer.titleColor).withBold(true))
                        );
                    }
                })
                .build());

        manager.add(
            InfoDisplay.Builder.<Boolean>begin()
                .setId("mia_coords").setName(Component.literal("MiA Coordinates"))
                .setDefaultState(true).setCodec(BuiltInConfigValueIOCodecs.BOOLEAN).setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
                .setCompiler((displayInfo, compiler, session, availableWidth, playerPos) -> {
                    if (displayInfo.getEffectiveState()) {
                        BlockPos MiAPos = MiaDeeperWorld.unwrap(playerPos);
                        String coords = MiAPos.getX() + ", " + MiAPos.getY() + ", " + MiAPos.getZ();

                        if (Minecraft.getInstance().font.width(coords) >= availableWidth) {
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
