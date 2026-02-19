package dev.hintsystem.miacompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class InventoryTracker {
    private static final Gson GSON = new Gson();

    static final int[] PASSIVE_SLOTS = {9, 10};

    public static HashMap<String, Integer> orthTrades = new HashMap<>();

    @Nullable
    public static Component onActionbarMessage(Component message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;

        List<Component> siblings = message.getSiblings();
        if (siblings.size() != 3) return null; // Cooldowns have 3 siblings

        List<Component> cooldownSiblings = siblings.get(2).getSiblings();
        if (cooldownSiblings.isEmpty()) return null;

        if (cooldownSiblings.size() == 1 && cooldownSiblings.getFirst().getString().contains("✔")) {
            return MiACompat.config.hideWeaponRelicCooldowns ? Component.empty() : null;
        }

        String cooldownText = cooldownSiblings.getLast().getString();
        float cooldownSeconds;
        try {
            cooldownSeconds = Float.parseFloat(cooldownText.strip().replaceAll("[\\[\\]s]", ""));
        } catch (NumberFormatException e) { return null; }

        String itemName = siblings.getFirst().getString();

        Stream.of(player.getMainHandItem(), player.getOffhandItem())
            .filter(stack -> !stack.isEmpty() && stack.getItemName().getString().equals(itemName))
            .findFirst()
            .ifPresent(stack -> {
                ItemCooldowns cooldowns = player.getCooldowns();

                if (!cooldowns.isOnCooldown(stack)) cooldowns.addCooldown(stack, (int) (cooldownSeconds * 20));
            });

        return MiACompat.config.hideWeaponRelicCooldowns ? Component.empty() : null;
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

    /** Returns the total coin worth of items in a container (whole coins only) */
    public static int getContainerCoinWorth(ItemStack itemStack) {
        Iterable<ItemStack> items = getContainerContents(itemStack);
        if (items == null) return 0;

        int coinSum = 0;
        for (ItemStack stack : items) {
            int nested = getContainerCoinWorth(stack);
            if (nested > 0) {
                coinSum += nested;
                continue;
            }

            Integer itemsPerCoin = InventoryTracker.getItemsPerCoin(stack);
            if (itemsPerCoin == null) continue;

            coinSum += Math.floorDiv(stack.getCount(), itemsPerCoin);
        }

        return coinSum;
    }

    /** Returns the exact coin worth of items in a container (includes fractional coins) */
    public static double getContainerExactCoinWorth(ItemStack itemStack) {
        Iterable<ItemStack> items = getContainerContents(itemStack);
        if (items == null) return 0;

        double coinSum = 0;
        for (ItemStack stack : items) {
            double nested = getContainerExactCoinWorth(stack);
            if (nested > 0) {
                coinSum += nested;
                continue;
            }

            Integer itemsPerCoin = InventoryTracker.getItemsPerCoin(stack);
            if (itemsPerCoin == null) continue;

            coinSum += (double) stack.getCount() / itemsPerCoin;
        }

        return coinSum;
    }

    /** Returns how many of this item are needed to trade for one coin, or null if not tradeable */
    @Nullable
    public static Integer getItemsPerCoin(ItemStack itemStack) {
        String modelName = getMiAModelName(itemStack);
        if (modelName == null) return null;

        return orthTrades.get(modelName);
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
        return  (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }

    static void loadFromFile() {
        orthTrades = loadOrthTrades();
    }

    static HashMap<String, Integer> loadOrthTrades() {
        try (InputStream stream = InventoryTracker.class.getResourceAsStream(
            "/assets/" + MiACompat.MOD_ID + "/config/orth_mob_trades.json")) {

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
