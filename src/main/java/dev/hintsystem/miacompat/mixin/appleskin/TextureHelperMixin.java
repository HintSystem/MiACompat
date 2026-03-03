package dev.hintsystem.miacompat.mixin.appleskin;

import dev.hintsystem.miacompat.MiACompat;

import squeek.appleskin.helpers.TextureHelper;
import squeek.appleskin.helpers.TextureHelper.FoodType;

import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TextureHelper.class, remap = false)
public class TextureHelperMixin {
	private static final @Unique Identifier miacompat$FOOD_HALF_HUNGER_TEXTURE = MiACompat.id("hud/food_half_hunger");
	private static final @Unique Identifier miacompat$FOOD_FULL_HUNGER_TEXTURE = MiACompat.id("hud/food_full_hunger");
	private static final @Unique Identifier miacompat$FOOD_EMPTY_TEXTURE = MiACompat.id("hud/food_empty");
	private static final @Unique Identifier miacompat$FOOD_HALF_TEXTURE = MiACompat.id("hud/food_half");
	private static final @Unique Identifier miacompat$FOOD_FULL_TEXTURE = MiACompat.id("hud/food_full");
	
    @Inject(method = "getFoodTexture", at = @At("HEAD"), cancellable = true)
    private static void overwriteGetFoodTexture(boolean isRotten, FoodType type, CallbackInfoReturnable<Identifier> cir) {
    	cir.setReturnValue(
            switch (type) {
                case EMPTY -> miacompat$FOOD_EMPTY_TEXTURE;
                case HALF -> isRotten ? miacompat$FOOD_HALF_HUNGER_TEXTURE : miacompat$FOOD_HALF_TEXTURE;
                case FULL -> isRotten ? miacompat$FOOD_FULL_HUNGER_TEXTURE : miacompat$FOOD_FULL_TEXTURE;
            }
    	);
    }
}
