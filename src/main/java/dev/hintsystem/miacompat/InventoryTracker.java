package dev.hintsystem.miacompat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class InventoryTracker {
    private static final Gson GSON = new Gson();

    static final int[] PASSIVE_SLOTS = {9, 10};

    public static HashMap<String, Integer> orthTrades = new HashMap<>();
    private static Map<String, FoodData> foodValues = new HashMap<>();

    /** Returns an iterable over the contents of a bundle or shulker box */
    @Nullable
    public static Iterable<ItemStack> getContainerContents(ItemStack itemStack) {
        BundleContents bundleContents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) return bundleContents.items();

        ItemContainerContents contents = itemStack.get(DataComponents.CONTAINER);
        if (contents != null) return contents.nonEmptyItems();

        return null;
    }

    public static class CoinWorth {
        public int whole = 0; // Amount you can sell for
        public double total = 0; // Accumulated total value
    }
    
    public static record FoodData(double saturation, double nutrition) {}

    public static CoinWorth getContainerCoinWorth(ItemStack itemStack) {
        CoinWorth worth = new CoinWorth();

        Iterable<ItemStack> items = getContainerContents(itemStack);
        if (items == null) return worth;

        for (ItemStack stack : items) {
            CoinWorth nested = getContainerCoinWorth(stack);
            if (nested.whole > 0 || nested.total > 0) {
                worth.whole += nested.whole;
                worth.total += nested.total;
                continue;
            }

            Integer itemsPerCoin = InventoryTracker.getItemsPerCoin(stack);
            if (itemsPerCoin == null) continue;

            worth.whole += Math.floorDiv(stack.getCount(), itemsPerCoin);
            worth.total += (double) stack.getCount() / itemsPerCoin;
        }

        return worth;
    }

    /** Returns how many of this item are needed to trade for one coin, or null if not tradeable */
    @Nullable
    public static Integer getItemsPerCoin(ItemStack itemStack) {
        String modelName = getMiAModelName(itemStack);
        if (modelName == null) return null;

        return orthTrades.get(modelName);
    }

    /** Returns FoodValue for this custom food, or null if not custom food */
    @Nullable
    public static FoodData getFoodValue(ItemStack itemStack) {
        String modelName = getMiAModelName(itemStack);
        return foodValues.get(modelName);
    }
    
    @Nullable
    public static Identifier getMiAModelId(ItemStack itemStack) {
        Identifier modelId = itemStack.get(DataComponents.ITEM_MODEL);
        if (modelId == null || !Objects.equals(modelId.getNamespace(), MiACompat.getMiANamespace())) return null;

        return modelId;
    }

    @Nullable
    public static String getMiAModelName(ItemStack itemStack) {
        Identifier modelId = itemStack.get(DataComponents.ITEM_MODEL);
        if (modelId == null || !Objects.equals(modelId.getNamespace(), MiACompat.getMiANamespace())) return null;

        return getModelName(modelId);
    }

    public static String getModelName(@NotNull Identifier modelId) {
        String path = modelId.getPath();

        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }

    static void loadFromFile() {
        orthTrades = loadOrthTrades();
        foodValues = loadFoodValues();
    }

    static HashMap<String, Integer> loadOrthTrades() {
        String orthTradesResource = "/assets/" + MiACompat.MOD_ID + "/config/orth_mob_trades.json";

        try (InputStream stream = MiACompat.class.getResourceAsStream(orthTradesResource)) {
            if (stream == null) {
                throw new RuntimeException("Could not find orth_mob_trades.json in resources!");
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

            Type type = new TypeToken<HashMap<String, Integer>>() {}.getType();

            return GSON.fromJson(reader, type);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load default prices", e);
        }
    }
    
    static HashMap<String, FoodData> loadFoodValues() {
        String foodValuesResource = "/assets/" + MiACompat.MOD_ID + "/config/food_values.json";

        try (InputStream stream = MiACompat.class.getResourceAsStream(foodValuesResource)) {
            if (stream == null) {
                throw new RuntimeException("Could not find food_values.json in resources!");
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

            Type type = new TypeToken<HashMap<String, FoodData>>() {}.getType();

            return GSON.fromJson(reader, type);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load food values", e);
        }
    }
    
}
