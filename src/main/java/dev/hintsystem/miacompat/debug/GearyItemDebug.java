package dev.hintsystem.miacompat.debug;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.GearyData;
import dev.hintsystem.miacompat.utils.ItemUtils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

public class GearyItemDebug {
    private static ItemStack lastItemStack = ItemStack.EMPTY;

    public static void onItemTooltip(ItemStack itemStack) {
        if (!ItemStack.matches(lastItemStack, itemStack)) {
            lastItemStack = itemStack;
            logItemInfo(itemStack);
        }
    }

    public static void logItemInfo(ItemStack itemStack) {
        StringBuilder itemInfo = new StringBuilder();

        GearyData.DataStore dataStore = GearyData.get(itemStack);

        if (dataStore != null) {
            itemInfo.append("\ndataStore:\n");
            for (var component : dataStore.tag.entrySet()) {
                itemInfo.append(component).append('\n');
            }

            Optional<byte[]> prefabsBytes = dataStore.getPrefabsBytes();
            if (prefabsBytes.isPresent()) {
                itemInfo.append("prefab-bytes: ");
                for (byte b : prefabsBytes.get()) {
                    itemInfo.append(String.format("%02X ", b));
                }
            }

            Set<Identifier> prefabs = dataStore.getPrefabs();
            if (!prefabs.isEmpty()) {
                itemInfo.append("\nprefabs: ");
                for (Identifier id : prefabs) {
                    itemInfo.append(id).append(" ");
                }
            }

        }

        itemInfo.append("\nmodel: ").append(ItemUtils.getMiAModelId(itemStack));

        MiACompat.LOGGER.info("\nitem: {}, {}\nlore: {} {}", itemStack, itemStack.getCustomName(),
            itemStack.get(DataComponents.LORE), itemInfo);
    }
}
