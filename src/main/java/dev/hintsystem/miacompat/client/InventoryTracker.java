package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    public static MutableComponent getContainerCoinWorthLabel(ItemStack itemStack) {
        return getContainerCoinWorthLabel(getContainerContents(itemStack));
    }

    /** @return Component with coin worth info, otherwise empty component if nothing to display */
    public static MutableComponent getContainerCoinWorthLabel(@Nullable Iterable<ItemStack> items) {
        CoinWorth coinWorth = getContainerCoinWorth(items);

        boolean showExactCoins = MiACompat.config.showPreciseCoinWorth;

        if ((showExactCoins && coinWorth.total == 0)
        || (!showExactCoins && coinWorth.whole == 0)) return Component.empty();

        MutableComponent containerTooltip = Component.literal(String.valueOf(coinWorth.whole)).withStyle(ChatFormatting.GOLD)
            .append(Component.literal(" $").withStyle(MiACompat.getIconStyle()));

        if (showExactCoins) containerTooltip.append(" (%.2f)".formatted(coinWorth.total));

        return containerTooltip;
    }

    public static class CoinWorth {
        public int whole = 0; // Amount you can sell for
        public double total = 0; // Accumulated total value
    }

    public static CoinWorth getContainerCoinWorth(@Nullable Iterable<ItemStack> items) {
        CoinWorth worth = new CoinWorth();
        if (items == null) return worth;

        for (ItemStack stack : items) {
            CoinWorth nested = getContainerCoinWorth(
                getContainerContents(stack)
            );

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

    /** Returns an iterable over the contents of a bundle or shulker box */
    @Nullable
    public static Iterable<ItemStack> getContainerContents(ItemStack itemStack) {
        BundleContents bundleContents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) return bundleContents.items();

        ItemContainerContents contents = itemStack.get(DataComponents.CONTAINER);
        if (contents != null) return contents.nonEmptyItems();

        return null;
    }

    /** Returns how many of this item are needed to trade for one coin, or null if not tradeable */
    @Nullable
    public static Integer getItemsPerCoin(ItemStack itemStack) {
        String modelName = getMiAModelName(itemStack);
        if (modelName == null) return null;

        return orthTrades.get(modelName);
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

    public static void loadFromFile() {
        orthTrades = loadOrthTrades();
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
}
