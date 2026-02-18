package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.InventoryTracker;
import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique private static ItemStack miacompat$lastContainerStack = ItemStack.EMPTY;
    @Unique private static Component miacompat$cachedContainerTooltip = null;

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void miacompat$modifyTooltipLore(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack itemStack = ((ItemStack)(Object) this);

        if (
            MiACompat.config.showContainerCoinWorth &&
            (itemStack.is(ItemTags.BUNDLES) || itemStack.is(ItemTags.SHULKER_BOXES))
        ) {
            if (!ItemStack.matches(miacompat$lastContainerStack, itemStack)) {
                miacompat$lastContainerStack = itemStack.copy();
                miacompat$cachedContainerTooltip = null;
            }

            if (miacompat$cachedContainerTooltip == null) {
                boolean showExactCoins = MiACompat.config.showContainerExactCoinWorth;

                int coinWorth = InventoryTracker.getContainerCoinWorth(itemStack);
                double exactCoinWorth = showExactCoins ? InventoryTracker.getContainerExactCoinWorth(itemStack) : 0;

                boolean skipNoValue = (showExactCoins && exactCoinWorth == 0) ||
                    (!showExactCoins && coinWorth == 0);

                if (skipNoValue) {
                    miacompat$cachedContainerTooltip = Component.empty();
                    return;
                }

                MutableComponent containerTooltip = Component.literal("  " + coinWorth).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" $").withStyle(MiACompat.getIconStyle()));

                if (showExactCoins) containerTooltip.append(" (%.2f)".formatted(exactCoinWorth));

                miacompat$cachedContainerTooltip = containerTooltip;
            }

            if (miacompat$cachedContainerTooltip.equals(Component.empty())) return;

            List<Component> tooltip = new ArrayList<>(cir.getReturnValue());

            Component titleLine = tooltip.getFirst();
            if (titleLine != null) {
                tooltip.set(0, titleLine.copy().append(miacompat$cachedContainerTooltip));
            }

            cir.setReturnValue(tooltip);
        }

        Integer tradeValue = InventoryTracker.getItemsPerCoin(itemStack);
        if (tradeValue == null) return;

        List<Component> tooltip = new ArrayList<>(cir.getReturnValue());

        MutableComponent firstLoreLine = tooltip.get(1).copy();
        if (firstLoreLine != null) {
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
                Component.literal(" (" + tradeValue + "/").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("$").withStyle(MiACompat.getIconStyle()))
                    .append(Component.literal(")"))
            ));
        }

        cir.setReturnValue(tooltip);
    }
}
