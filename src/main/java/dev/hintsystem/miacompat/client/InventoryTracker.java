package dev.hintsystem.miacompat.client;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.GearyData;
import dev.hintsystem.miacompat.server.ServerItemRegistry;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;
import dev.hintsystem.miacompat.server.config.geary.item.RelicConfig;
import dev.hintsystem.miacompat.utils.ItemUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

public class InventoryTracker {
    private static final Gson GSON = new Gson();
    private static final Path SAVE_PATH = MiACompat.CONFIG_FOLDER.resolve("compendium.json");

    public static final int CHECK_UNLOCKED_RELICS_TICKS = 24;
    public static final int[] PASSIVE_RELIC_SLOTS = {9, 10};

    public static Map<Identifier, Integer> orthTrades = new HashMap<>();
    public static CompendiumUnlocks compendium = new CompendiumUnlocks();

    public static int relicTicks = 0;

    public static class CompendiumUnlocks {
        public Set<String> relics = new HashSet<>();
        public Map<String, Integer> mobs = new HashMap<>();
    }

    public static void tick(Minecraft client) {
        relicTicks = (relicTicks + 1) % CHECK_UNLOCKED_RELICS_TICKS;

        LocalPlayer player = client.player;
        if (player == null || relicTicks != 0) return;

        for (ItemStack itemStack : ItemUtils.iterateContainedItems(player.getInventory())) {
            ItemConfig item = ServerItemRegistry.getItem(itemStack);
            if (!(item instanceof RelicConfig relic)) continue;

            if (compendium.relics.add(relic.prefabId.toString()))
                saveCompendium();
        }
    }

    public static MutableComponent getContainerCoinWorthLabel(ItemStack itemStack) {
        return getContainerCoinWorthLabel(ItemUtils.getContainerContents(itemStack));
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

        for (ItemStack stack : ItemUtils.iterateContainedItems(items)) {
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
        Identifier prefabId = GearyData.getFirstPrefabId(itemStack);
        if (prefabId == null) return null;

        return orthTrades.get(prefabId);
    }

    public static void saveCompendium() {
        try {
            Files.createDirectories(SAVE_PATH.getParent());
            Files.writeString(SAVE_PATH, GSON.toJson(compendium));
        } catch (IOException e) {
            MiACompat.LOGGER.error("Failed to save compendium", e);
        }
    }

    public static void loadCompendium() {
        if (!Files.isRegularFile(SAVE_PATH)) return;

        try {
            CompendiumUnlocks data = GSON.fromJson(Files.readString(SAVE_PATH), CompendiumUnlocks.class);

            if (data != null) compendium = data;
        } catch (IOException e) {
            MiACompat.LOGGER.error("Failed to load compendium", e);
        }
    }

    public static void loadFromResources(ResourceManager resourceManager) {
        orthTrades = loadOrthTrades(resourceManager);
    }

    private static Map<Identifier, Integer> loadOrthTrades(ResourceManager resourceManager) {
        Identifier id = MiACompat.id("config/orth_mob_trades.json");

        try (InputStream stream = resourceManager.getResourceOrThrow(id).open()) {
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);

            Type type = new TypeToken<LinkedHashMap<String, Integer>>() {}.getType();
            Map<String, Integer> raw = GSON.fromJson(reader, type);

            Map<Identifier, Integer> result = new LinkedHashMap<>();
            for (var entry : raw.entrySet()) {
                result.put(Identifier.parse(entry.getKey()), entry.getValue());
            }

            MiACompat.LOGGER.info("Loaded {} orth mob drop trades", result.size());

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + id, e);
        }
    }
}
