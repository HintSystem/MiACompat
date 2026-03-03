package dev.hintsystem.miacompat.mixin;

import dev.hintsystem.miacompat.client.tooltip.OrthWorthTooltip;

import net.minecraft.network.chat.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void miacompat$modifyTooltipLore(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack itemStack = ((ItemStack)(Object) this);
        List<Component> tooltip = cir.getReturnValue();

        if ((itemStack.is(ItemTags.BUNDLES) || itemStack.is(ItemTags.SHULKER_BOXES))) {
            OrthWorthTooltip.modifyContainerTooltip(itemStack, tooltip);
            return;
        }

        OrthWorthTooltip.modifyItemTooltip(itemStack, tooltip);
    }
}
