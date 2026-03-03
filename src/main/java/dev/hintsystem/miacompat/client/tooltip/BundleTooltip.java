package dev.hintsystem.miacompat.client.tooltip;

import dev.hintsystem.miacompat.MiACompat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import org.joml.Vector2i;

import java.util.List;

public abstract class BundleTooltip {
    // Add lore to item tooltips drawn in bundles
    public static boolean onDrawItemTooltip(BundleContents contents, Font font, GuiGraphics graphics, int x, int y, int w) {
        if (!MiACompat.config.showItemLoreInBundles || !contents.hasSelectedItem()) return false;

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

        graphics.renderTooltip(font, tooltipComponents,
            x, y - 19,
            (screenWidth, screenHeight, xT, yT, widthT, heightT) -> {
                int centerOffset = (w - widthT) / 2;
                if (widthT > w) centerOffset = 0;

                Vector2i result = new Vector2i(xT + centerOffset, yT - heightT);
                if (result.x + widthT > screenWidth) {
                    result.x = Math.max(xT + w - widthT, 4);
                }

                return result;
            },
            itemStack.get(DataComponents.TOOLTIP_STYLE)
        );

        return true;
    }
}
