package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.InventoryTracker;
import dev.hintsystem.miacompat.InventoryTracker.FoodData;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique private static ItemStack miacompat$lastContainerStack = ItemStack.EMPTY;
    @Unique private static Component miacompat$cachedContainerTooltip = null;
    
    private static final DecimalFormat FOOD_DF = new DecimalFormat("0.#");

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

                InventoryTracker.CoinWorth coinWorth = InventoryTracker.getContainerCoinWorth(itemStack);

                boolean skipNoValue = (showExactCoins && coinWorth.total == 0) ||
                    (!showExactCoins && coinWorth.whole == 0);

                if (skipNoValue) {
                    miacompat$cachedContainerTooltip = Component.empty();
                    return;
                }

                MutableComponent containerTooltip = Component.literal("  " + coinWorth.whole).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" $").withStyle(MiACompat.getIconStyle()));

                if (showExactCoins) containerTooltip.append(" (%.2f)".formatted(coinWorth.total));

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
        if (tradeValue != null) {
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
        
        FoodData foodValue = InventoryTracker.getFoodValue(itemStack);
        if (foodValue != null && MiACompat.config.showFoodSaturationValues) {
        	List<Component> tooltip = new ArrayList<>(cir.getReturnValue());
        	
        	List<Component> foodTip = new ArrayList<>();

        	foodTip.add(Component.literal(""));
        	foodTip.add(Component.literal("When eaten:").withStyle(ChatFormatting.GRAY));
        	foodTip.add(Component.literal(" " + FOOD_DF.format(foodValue.nutrition()) + " Nutrition").withStyle(ChatFormatting.GOLD));
        	int hitPointsRestorable = (int) Math.floor(foodValue.saturation() / 1.5D);
        	foodTip.add(Component.literal(" " + FOOD_DF.format(foodValue.saturation()) + " Saturation" + (hitPointsRestorable == 0 ? "" : " worth "
        			+ hitPointsRestorable + " Health")).withStyle(ChatFormatting.GOLD));
        	int index = tooltip.size();
        	for (int i = 0; i < tooltip.size(); i++) {
        		if (tooltip.get(i).getString().contains("minecraft:")) {
        			index = i;
        			break;
        		}
        	}
        	
        	tooltip.addAll(index, foodTip);
        	
            cir.setReturnValue(tooltip);
        }
    }
    
}
