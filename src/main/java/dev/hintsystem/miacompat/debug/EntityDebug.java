package dev.hintsystem.miacompat.debug;

import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import org.jetbrains.annotations.Nullable;

public class EntityDebug {
    @Nullable
    public static EntityHitResult raycast(LocalPlayer player, double range) {
        if (player == null) return null;

        Vec3 look = player.getLookAngle().scale(range);
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(look.scale(range));

        return ProjectileUtil.getEntityHitResult(player,
            start, end,
            player.getBoundingBox().expandTowards(look),
            Entity::isPickable,
            range * range
        );
    }

    public static EntityTreeBuilder logEntityInfo(Level level, Entity entity) {
        if (level == null || entity == null) return null;

        EntityTreeBuilder tree = new EntityTreeBuilder()
            .appendEntity(entity);

        Entity rootVehicle = entity.getRootVehicle();
        if (rootVehicle != entity) {
            tree.section("ROOT VEHICLE");

            tree.beginChild(false);
            tree.appendEntityTree(rootVehicle);
            tree.endChild();
        }

        if (entity instanceof Interaction interaction) {
            List<Entity> entitiesNear = level.getEntities(interaction, interaction.getBoundingBox().inflate(3));

            tree.section("NEARBY ROOTS");

            tree.beginChild(false);
            for (Entity near : entitiesNear) {
                if (!(near instanceof Display.ItemDisplay)) continue;
                if (tree.visited().contains(near.getUUID())) continue;

                Entity nearRoot = near.getRootVehicle();
                tree.appendEntityTree(nearRoot);
            }
            tree.endChild();
        }

        MiACompat.LOGGER.info("\n{}", tree.toString());
        return tree;
    }

    public static String unknownEntityId(int id) {
        return "Unknown entity, id: " + id;
    }

    public static String describe(Entity entity) {
        StringBuilder s = new StringBuilder();
        s.append(String.format("'%s'/%d", entity.getPlainTextName(), entity.getId()));

        if (entity.getRemovalReason() != null) {
            s.append(" (").append(entity.getRemovalReason()).append(")");
        }

        if (entity instanceof Display.ItemDisplay display) {
            Identifier model = display.getItemStack().get(DataComponents.ITEM_MODEL);
            ItemStack stack = display.getItemStack();

            s.append(" [");

            if (model != null) {
                s.append(model);
            } else if (!stack.isEmpty()) {
                s.append(stack.getItem());
            } else {
                s.append("empty");
            }

            s.append("]");
        }

        return s.toString();
    }

    public static class EntityTreeBuilder {
        private final StringBuilder builder = new StringBuilder();

        private final Deque<Boolean> stack = new ArrayDeque<>();
        private final Set<UUID> visitedEntityIds = new LinkedHashSet<>();

        public Set<UUID> visited() { return visitedEntityIds; }

        public EntityTreeBuilder appendEntityTree(Entity entity) {
            appendEntity(entity, false);

            var passengers = entity.getPassengers();
            if (!passengers.isEmpty())
                detail("passangers", "");

            for (int i = 0; i < passengers.size(); i++) {
                beginChild(i == passengers.size() - 1);
                appendEntityTree(passengers.get(i));
                endChild();
            }

            return this;
        }

        public EntityTreeBuilder appendEntityList(List<Entity> entities) {
            for (int i = 0; i < entities.size(); i++) {
                beginChild(i == entities.size() - 1);
                appendEntity(entities.get(i));
                endChild();
            }

            return this;
        }

        public EntityTreeBuilder appendEntity(Entity entity) { return appendEntity(entity, true); }

        private EntityTreeBuilder appendEntity(Entity entity, boolean includeVehicle) {
            visitedEntityIds.add(entity.getUUID());

            line(describe(entity));

            detail("pos", entity.position());

            Set<String> components = getContainedComponents(entity);
            if (!components.isEmpty())
                detail("components", getContainedComponents(entity));

            CustomData data = entity.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty())
                detail("custom", data);

            detail("sync", getEntityData(entity));

            if (includeVehicle && entity.getVehicle() != null) {
                detail("vehicle", describe(entity.getVehicle()));
            }

            return this;
        }

        private void beginChild(boolean last) {
            stack.push(last);
        }

        private void endChild() {
            stack.pop();
        }

        public EntityTreeBuilder section(Object text) {
            line("");
            appendIndent(false);
            builder.append(">> [").append(text).append("] <<\n");

            return this;
        }

        public EntityTreeBuilder line(Object text) {
            appendIndent(true);
            builder.append(text).append('\n');

            return this;
        }

        public EntityTreeBuilder detail(String key, Object value) {
            appendIndent(false);
            builder.append(key).append(": ")
                .append(value).append('\n');

            return this;
        }

        private void appendIndent(boolean branch) {
            if (stack.isEmpty()) return;

            Boolean[] levels = stack.toArray(Boolean[]::new);

            for (int i = levels.length - 1; i >= 0; i--) {
                boolean last = levels[i];

                if (i == 0) {
                    builder.append(branch
                        ? (last ? "└─ " : "├─ ")
                        : (last ? "   " : "│  "));
                } else {
                    builder.append(last ? "   " : "│  ");
                }
            }
        }

        @Override
        public String toString() { return builder.toString().stripTrailing(); }
    }

    public static List<Map<String, String>> getEntityData(Entity entity) {
        return getEntityData(entity, entity.getEntityData().getNonDefaultValues());
    }

    public static List<Map<String, String>> getEntityData(
        Entity entity,
        List<SynchedEntityData.DataValue<?>> entityData
    ) {
        if (entityData == null) return List.of();

        List<Map<String, String>> formattedData = new ArrayList<>();

        for (SynchedEntityData.DataValue<?> dataValue : entityData) {
            Map<String, String> dataInfo = new LinkedHashMap<>();

            dataInfo.put("i", getDataName(entity, dataValue.id()));

            String value = dataValue.id() == 0
                ? getSharedFlags((byte) dataValue.value()).toString()
                : dataValue.value().toString();

            dataInfo.put("value", value);

            formattedData.add(dataInfo);
        }

        return formattedData;
    }

    private static List<String> getSharedFlags(byte flags) {
        List<String> enabled = new ArrayList<>();

        if ((flags & (1 << 0)) != 0) enabled.add("0=ON_FIRE");
        if ((flags & (1 << 1)) != 0) enabled.add("1=SHIFT_KEY_DOWN");
        if ((flags & (1 << 3)) != 0) enabled.add("3=SPRINTING");
        if ((flags & (1 << 5)) != 0) enabled.add("5=INVISIBLE");
        if ((flags & (1 << 6)) != 0) enabled.add("6=GLOWING");
        if ((flags & (1 << 7)) != 0) enabled.add("7=FALL_FLYING");

        return enabled;
    }

    /** {@link Entity#Entity} */
    private static final Map<Integer, String> entityDataIdToName = Map.of(
        0, "SHARED_FLAGS_ID",
        1, "AIR_SUPPLY_ID",
        2, "CUSTOM_NAME",
        3, "CUSTOM_NAME_VISIBLE",
        4, "SILENT",
        5, "NO_GRAVITY",
        6, "POSE",
        7, "TICKS_FROZEN"
    );

    /** {@link Interaction} */
    private static final Map<Integer, String> interactionDataIdToName = Map.of(
        8, "WIDTH_ID",
        9, "HEIGHT_ID",
        10, "RESPONSE_ID"
    );

    /** {@link Display} */
    private static final Map<Integer, String> displayDataIdToName = Map.ofEntries(
        Map.entry(11, "TRANSLATION_ID"),
        Map.entry(12, "SCALE_ID"),
        Map.entry(13, "LEFT_ROTATION_ID"),
        Map.entry(14, "RIGHT_ROTATION_ID"),
        Map.entry(15, "BILLBOARD_RENDER_CONSTRAINTS_ID"),
        Map.entry(16, "BRIGHTNESS_OVERRIDE_ID"),
        Map.entry(17, "VIEW_RANGE_ID"),
        Map.entry(18, "SHADOW_RADIUS_ID"),
        Map.entry(19, "SHADOW_STRENGTH_ID"),
        Map.entry(20, "WIDTH_ID"),
        Map.entry(21, "HEIGHT_ID"),
        Map.entry(22, "GLOW_COLOR_OVERRIDE_ID")
    );

    private static final Map<Integer, String> itemDisplayDataIdToName = Map.of(
        23, "ITEM_STACK_ID",
        24, "ITEM_DISPLAY_ID"
    );

    private static String getDataName(Entity entity, int id) {
        if (entity instanceof Display.ItemDisplay) {
            String name = itemDisplayDataIdToName.get(id);
            if (name != null) return name;
        }

        if (entity instanceof Display) {
            String name = displayDataIdToName.get(id);
            if (name != null) return name;
        }

        if (entity instanceof Interaction) {
            String name = interactionDataIdToName.get(id);
            if (name != null) return name;
        }

        return entityDataIdToName.getOrDefault(id, "ID_" + id);
    }

    public static Set<String> getContainedComponents(DataComponentGetter componentContainer) {
        Set<String> components = new HashSet<>();
        BuiltInRegistries.DATA_COMPONENT_TYPE.forEach((componentType) -> {
            var data = componentContainer.get(componentType);
            if (data == null) return;

            if (data instanceof CustomData customData && customData.isEmpty()) return;

            components.add(componentType.toString());
        });

        return components;
    }
}
