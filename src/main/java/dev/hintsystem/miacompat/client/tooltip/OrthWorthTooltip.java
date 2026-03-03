package dev.hintsystem.miacompat.client.tooltip;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.InventoryTracker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public abstract class OrthWorthTooltip {
    private static ItemStack lastContainerStack = ItemStack.EMPTY;
    private static Component cachedContainerTooltip = null;

    public static void modifyContainerTooltip(ItemStack itemStack, List<Component> tooltip) {
        if (!MiACompat.config.showContainerCoinWorth) return;

        if (!ItemStack.matches(lastContainerStack, itemStack)) {
            lastContainerStack = itemStack.copy();
            cachedContainerTooltip = null;
        }

        if (cachedContainerTooltip == null) {
            boolean showExactCoins = MiACompat.config.showContainerExactCoinWorth;

            InventoryTracker.CoinWorth coinWorth = InventoryTracker.getContainerCoinWorth(itemStack);

            boolean skipNoValue = (showExactCoins && coinWorth.total == 0) ||
                (!showExactCoins && coinWorth.whole == 0);

            if (skipNoValue) {
                cachedContainerTooltip = Component.empty();
                return;
            }

            MutableComponent containerTooltip = Component.literal("  " + coinWorth.whole).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" $").withStyle(MiACompat.getIconStyle()));

            if (showExactCoins) containerTooltip.append(" (%.2f)".formatted(coinWorth.total));

            cachedContainerTooltip = containerTooltip;
        }

        if (cachedContainerTooltip.equals(Component.empty())) return;

        Component titleLine = tooltip.getFirst();
        if (titleLine != null) {
            tooltip.set(0, titleLine.copy().append(cachedContainerTooltip));
        }
    }

    public static void modifyItemTooltip(ItemStack itemStack, List<Component> tooltip) {
        Integer tradeValue = InventoryTracker.getItemsPerCoin(itemStack);
        if (tradeValue == null || tooltip.size() < 2) return;

        MutableComponent firstLoreLine = tooltip.get(1).copy();
        List<Component> siblings = firstLoreLine.getSiblings();

        // Remove orth coin and replace with value
        if (!siblings.isEmpty()) {
            String lastText = siblings.getLast().getString();

            if (lastText.equals("\uE000")) siblings.removeLast();
        } else {
            firstLoreLine = Component.literal(
                firstLoreLine.getString().replace(":cosmetic_orth_coin:", "")
            ).setStyle(firstLoreLine.getStyle());

        }

        tooltip.set(1, firstLoreLine.append(
            Component.literal(" (" + tradeValue + "/").withStyle(ChatFormatting.GOLD).withoutShadow()
                .append(Component.literal("$").withStyle(MiACompat.getIconStyle()))
                .append(Component.literal(")"))
        ));
    }
}
