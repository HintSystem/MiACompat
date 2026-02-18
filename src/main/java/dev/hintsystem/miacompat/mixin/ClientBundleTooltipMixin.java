package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.MiACompat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;

import org.joml.Vector2i;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin {
    @Final @Shadow
    private BundleContents contents;

    @Inject(method = "drawSelectedItemTooltip", at = @At("HEAD"), cancellable = true)
    private void miacompat$drawSelectedItemTooltipWithDescription(
        Font font,
        GuiGraphics graphics,
        int x,
        int y,
        int w,
        CallbackInfo ci
    ) {
        if (!MiACompat.config.showItemLoreInBundles) return;

        if (contents.hasSelectedItem()) {
            ItemStack itemStack = contents.getItemUnsafe(contents.getSelectedItem());

            List<Component> tooltipLines = itemStack.getTooltipLines(
                Item.TooltipContext.EMPTY,
                null,
                TooltipFlag.NORMAL
            );

            List<ClientTooltipComponent> tooltipComponents = tooltipLines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();

            graphics.renderTooltip(
                font,
                tooltipComponents,
                x,
                y - 19,
                (screenWidth, screenHeight, xT, yT, wT, hT) -> {
                    int centerOffset = (w - wT) / 2;
                    if (wT > w) centerOffset = 0;

                    Vector2i result = new Vector2i(xT + centerOffset, yT - hT);
                    if (result.x + wT > screenWidth) {
                        result.x = Math.max(xT + w - wT, 4);
                    }

                    return result;
                },
                itemStack.get(DataComponents.TOOLTIP_STYLE)
            );

            ci.cancel();
        }
    }
}
