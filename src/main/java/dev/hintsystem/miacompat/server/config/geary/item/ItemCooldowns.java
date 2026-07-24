package dev.hintsystem.miacompat.server.config.geary.item;

import dev.hintsystem.miacompat.server.config.geary.ItemYamlSchema;

import org.jetbrains.annotations.Nullable;

public class ItemCooldowns {
    @Nullable
    public final ActionCooldown leftClick;
    @Nullable
    public final ActionCooldown rightClick;

    public ItemCooldowns(@Nullable ActionCooldown leftClick, @Nullable ActionCooldown rightClick) {
        this.leftClick = leftClick;
        this.rightClick = rightClick;
    }

    @Nullable
    public static ItemCooldowns fromItemConfig(ItemYamlSchema itemConfig) {
        ItemYamlSchema.Observe observe = itemConfig.observe;
        if (observe == null) return null;

        ActionCooldown leftClick = ActionCooldown.fromActionsConfig(observe.itemLeftClick);
        ActionCooldown rightClick = ActionCooldown.fromActionsConfig(observe.itemRightClick);

        if (leftClick == null && rightClick == null) return null;
        return new ItemCooldowns(leftClick, rightClick);
    }
}
