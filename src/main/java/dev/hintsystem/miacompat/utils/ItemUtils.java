package dev.hintsystem.miacompat.utils;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.config.geary.item.ItemConfig;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /** Describes an item without relying on its type, so custom items can be differentiated */
    public static String itemDescriptor(ItemConfig item) {
        return String.format("'%s' with model '%s'", item.getOriginal().getItem().itemName, item.modelId);
    }

    /** @return Iterable over the contents of a bundle or shulker box */
    @Nullable
    public static Iterable<ItemStack> getContainerContents(ItemStack itemStack) {
        BundleContents bundleContents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) return bundleContents.items();

        ItemContainerContents contents = itemStack.get(DataComponents.CONTAINER);
        if (contents != null) return contents.nonEmptyItems();

        return null;
    }

    /**
     * Iterates over the passed items, and also any additional container items.
     * Containers themselves are skipped
     */
    public static Iterable<ItemStack> iterateContainedItems(Iterable<ItemStack> items) {
        return () -> new ContainedItemIterator(items.iterator());
    }

    private static class ContainedItemIterator implements Iterator<ItemStack> {
        private final Deque<Iterator<ItemStack>> stack = new ArrayDeque<>();
        private ItemStack next;

        ContainedItemIterator(Iterator<ItemStack> root) {
            stack.push(root);
            advance();
        }

        private void advance() {
            next = null;

            while (!stack.isEmpty()) {
                Iterator<ItemStack> it = stack.peek();

                if (!it.hasNext()) {
                    stack.pop();
                    continue;
                }

                ItemStack stackItem = it.next();

                Iterable<ItemStack> contents = getContainerContents(stackItem);
                if (contents != null) {
                    stack.push(contents.iterator());
                } else {
                    next = stackItem;
                    return;
                }
            }
        }

        @Override
        public boolean hasNext() { return next != null; }

        @Override
        public ItemStack next() {
            if (next == null)
                throw new NoSuchElementException();

            ItemStack result = next;
            advance();
            return result;
        }
    }
}
