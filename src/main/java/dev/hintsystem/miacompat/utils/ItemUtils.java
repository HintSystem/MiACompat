package dev.hintsystem.miacompat.utils;

import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class ItemUtils {
    public static Identifier getMiAModelId(ItemStack itemStack) {
        Identifier modelId = itemStack.get(DataComponents.ITEM_MODEL);
        if (modelId == null || !Objects.equals(modelId.getNamespace(), MiACompat.getMiANamespace())) return null;

        return modelId;
    }

    @Nullable
    public static String getMiAModelName(ItemStack itemStack) {
        Identifier modelId = getMiAModelId(itemStack);
        return modelId != null ? getModelName(modelId) : null;
    }

    public static String getModelName(@NotNull Identifier modelId) {
        String path = modelId.getPath();

        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
    }

    /** Describes an item without relying on its type, so custom items can be differentiated */
    public static String itemDescriptor(ItemStack item) {
        Identifier modelId = item.get(DataComponents.ITEM_MODEL);
        return String.format("'%s' with model '%s'", item.getHoverName(), modelId);
    }
}
