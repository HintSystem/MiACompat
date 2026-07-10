package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.InventoryTracker;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    @Shadow @Final protected AbstractContainerMenu menu;

    @Shadow protected int imageWidth;
    @Shadow protected int titleLabelY;

    @Unique
    private Component miacompat$cachedWorthLabel = Component.empty();

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "containerTick", at = @At("HEAD"))
    protected void miacompat$containerTick(CallbackInfo ci) {
        Container container = null;
        if (this.menu instanceof ChestMenu chest) {
            container = chest.getContainer();
        } else if (this.menu instanceof ShulkerBoxMenu shulker) {
            container = ((ShulkerBoxMenuAccessor) shulker).miacompat$getContainer();
        }

        if (container == null) return;

        miacompat$cachedWorthLabel = InventoryTracker.getContainerCoinWorthLabel(container);
    }

    @Inject(method = "renderLabels", at = @At("HEAD"))
    public void miacompat$renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!MiACompat.config.showCoinWorthInContainers) return;

        if (miacompat$cachedWorthLabel == null
            || miacompat$cachedWorthLabel.equals(Component.empty())) return;

        int labelX = this.imageWidth - this.font.width(miacompat$cachedWorthLabel) - 6;

        guiGraphics.drawString(
            this.font,
            Component.empty()
                .append(miacompat$cachedWorthLabel)
                .setStyle(Style.EMPTY.withShadowColor(0xA6000000)),
            labelX, this.titleLabelY, -12566464, false
        );
    }
}
