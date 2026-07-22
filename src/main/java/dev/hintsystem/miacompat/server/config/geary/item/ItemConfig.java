package dev.hintsystem.miacompat.server.config.geary.item;

import dev.hintsystem.miacompat.client.CooldownTracker;
import dev.hintsystem.miacompat.server.MiniMessageParser;
import dev.hintsystem.miacompat.server.config.geary.ItemYamlSchema;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class ItemConfig {
    private final ItemYamlSchema original;

    public final Identifier prefabId;
    public final Item type;
    public final Component name;
    public final Identifier modelId;
    public final List<Component> lore;

    @Nullable
    public final CooldownTracker.GearCooldowns gearCooldowns;

    ItemConfig(
        ItemYamlSchema original, Identifier prefabId,
        Item type, Component name, Identifier modelId, List<Component> lore, @Nullable CooldownTracker.GearCooldowns gearCooldowns
    ) {
        this.original = original;
        this.prefabId = prefabId;
        this.type = type;
        this.name = name;
        this.modelId = modelId;
        this.lore = lore;
        this.gearCooldowns = gearCooldowns;
    }

    public ItemYamlSchema getOriginal() {
        return original;
    }

    public static ItemConfig parse(Identifier prefabId, ItemYamlSchema itemConfig) throws Exception {
        ItemYamlSchema.Item item = itemConfig.getItem();

        Identifier itemModel = Identifier.tryParse(item.itemModel);
        if (itemModel == null) throw new IllegalStateException("Not a valid itemModel");

        Item type = BuiltInRegistries.ITEM.get(Identifier.parse(item.type))
            .orElseThrow(() -> new IllegalStateException("Not a valid item type")).value();

        List<Component> lore = new ArrayList<>();
        if (item.lore != null) {
            for (String line : item.lore) {
                lore.add(MiniMessageParser.parse(line));
            }
        }

        return new ItemConfig(
            itemConfig, prefabId,
            type, MiniMessageParser.parse(item.itemName), itemModel, lore, CooldownTracker.GearCooldowns.fromItemConfig(itemConfig)
        );
    }
}
