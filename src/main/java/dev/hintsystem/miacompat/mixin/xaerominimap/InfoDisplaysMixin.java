package dev.hintsystem.miacompat.mixin.xaerominimap;

import dev.hintsystem.miacompat.server.ServerLayerRegistry;
import dev.hintsystem.miacompat.server.config.LayerConfig;
import dev.hintsystem.miacompat.server.config.LayerMeta;
import dev.hintsystem.miacompat.server.config.SectionYamlSchema;
import dev.hintsystem.miacompat.utils.DeeperWorld;

import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.InfoDisplayManager;
import xaero.hud.minimap.info.InfoDisplays;
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories;
import xaero.lib.common.config.option.value.io.serialization.BuiltInConfigValueIOCodecs;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    private void miacompat$onConstructed(CallbackInfo ci) {
        manager.add(
            InfoDisplay.Builder.<Boolean>begin()
                .setId("mia_layer").setName(Component.literal("MiA Layer"))
                .setDefaultState(true).setCodec(BuiltInConfigValueIOCodecs.BOOLEAN).setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
                .setCompiler((displayInfo, compiler, session, availableWidth, playerPos) -> {
                    if (displayInfo.getEffectiveState()) {

                        SectionYamlSchema.Section section = ServerLayerRegistry.getSectionForPosition(playerPos);
                        if (section == null) return;

                        LayerConfig layer = ServerLayerRegistry.getLayer(section);
                        if (layer == null) return;

                        if (layer.meta != LayerMeta.Orth) {
                            int sectionIndex = layer.getSectionIndex(section) + 1;
                            compiler.addLine(
                                Component.literal("L" + layer.meta.ordinal() + "S" + sectionIndex)
                                    .setStyle(Style.EMPTY.withColor(layer.getSubtitleColor()))
                            );
                        }

                        compiler.addLine(
                            Component.literal(layer.name.getString())
                                .setStyle(Style.EMPTY.withColor(layer.getTitleColor()).withBold(true))
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
                        BlockPos MiAPos = DeeperWorld.unwrap(playerPos);
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
