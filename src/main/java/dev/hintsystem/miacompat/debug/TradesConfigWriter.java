package dev.hintsystem.miacompat.debug;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.GearyData;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TradesConfigWriter {
    private static final Path DATA_FILE = MiACompat.CONFIG_FOLDER.resolve("orth_mob_trades.json");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private static final HashMap<String, Integer> orthTrades = new LinkedHashMap<>();

    public static void onMerchantMenu(MerchantMenu menu) {
        for (MerchantOffer offer : menu.getOffers()) {
            ItemStack baseCost = offer.getBaseCostA();

            Identifier prefabId = GearyData.getFirstPrefabId(baseCost);
            if (prefabId == null) continue;

            MiACompat.LOGGER.info("offer - item: {}, id: {}, cost: {}", baseCost.getItemName().getString(), prefabId, baseCost.getCount());

            orthTrades.put(prefabId.toString(), baseCost.getCount());
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            Files.writeString(DATA_FILE, GSON.toJson(orthTrades));
        } catch (IOException e) {
            MiACompat.LOGGER.error("Failed to save trades", e);
        }
    }
}
