package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.CooldownTracker;
import dev.hintsystem.miacompat.client.CooldownTracker.GearCooldowns;
import dev.hintsystem.miacompat.client.InventoryTracker;
import dev.hintsystem.miacompat.server.schema.ItemConfigSchema;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerItemRegistry {
    private static final Map<Identifier, ItemConfig> itemConfigByItemModel = new HashMap<>();

    public static Map<Identifier, ItemConfig> getAllItems() {
        return Collections.unmodifiableMap(itemConfigByItemModel);
    }

    @Nullable
    public static ItemConfig getItem(ItemStack item) {
        Identifier modelId = InventoryTracker.getMiAModelId(item);
        if (modelId == null) return null;

        return getItem(modelId);
    }

    @Nullable
    public static ItemConfig getItem(Identifier modelId) { return itemConfigByItemModel.get(modelId); }

    public enum RelicGrade {
        I(Component.literal("Grade I").withColor(0xFFE5663D)),
        II(Component.literal("Grade II").withColor(0xFF43583C)),
        III(Component.literal("Grade III").withColor(0xFFD7C9B3)),
        IV(Component.literal("Grade IV").withColor(0xFFBB9672));

        public final Component displayName;

        RelicGrade(Component displayName) {
            this.displayName = displayName;
        }
    }

    public static class RelicConfig extends ItemConfig {
        private static final Pattern GRADE_PATTERN =
            Pattern.compile("\\bGrade\\s+(I|II|III|IV)\\b");

        public final RelicGrade grade;

        private RelicConfig(RelicGrade grade, ItemConfig itemConfig) {
            super(itemConfig.original, itemConfig.type, itemConfig.name, itemConfig.modelId, itemConfig.lore, itemConfig.gearCooldowns);
            this.grade = grade;
        }

        private static boolean isRelic(Path relative) {
            return relative.startsWith("relics");
        }

        @Nullable
        private static RelicGrade extractGrade(String infoLine) {
            Matcher m = GRADE_PATTERN.matcher(infoLine);
            return m.find() ? RelicGrade.valueOf(m.group(1)) : null;
        }

        /** @return null, if item is not a relic */
        @Nullable
        public static RelicConfig tryParse(ItemConfigSchema itemConfig) throws Exception {
            String infoLine = itemConfig.getItem().lore.getFirst();
            RelicGrade grade = extractGrade(infoLine);

            return grade != null
                ? new RelicConfig(grade, ItemConfig.parse(itemConfig))
                : null;
        }

        public static RelicConfig parse(ItemConfigSchema itemConfig) throws Exception {
            String infoLine = itemConfig.getItem().lore.getFirst();
            RelicGrade grade = extractGrade(infoLine);

            if (grade == null)
                throw new IllegalStateException("Invalid relic grade: " + infoLine);

            return new RelicConfig(grade, ItemConfig.parse(itemConfig));
        }
    }

    public static class ItemConfig {
        private final ItemConfigSchema original;

        public final Item type;
        public final Component name;
        public final Identifier modelId;
        public final List<Component> lore;

        @Nullable public final GearCooldowns gearCooldowns;

        private ItemConfig(
            ItemConfigSchema original,
            Item type, Component name, Identifier modelId, List<Component> lore, @Nullable GearCooldowns gearCooldowns
        ) {
            this.original = original;
            this.type = type;
            this.name = name;
            this.modelId = modelId;
            this.lore = lore;
            this.gearCooldowns = gearCooldowns;
        }

        public ItemConfigSchema getOriginal() { return original; }

        private static ItemConfig parse(ItemConfigSchema itemConfig) throws Exception {
            ItemConfigSchema.Item item = itemConfig.getItem();

            Identifier itemModel = Identifier.tryParse(item.itemModel);
            if (itemModel == null) throw new IllegalStateException("Not a valid itemModel");

            Item type = BuiltInRegistries.ITEM.get(Identifier.parse(item.type))
                .orElseThrow(() -> new IllegalStateException("Not a valid item type")).value();

            List<Component> lore = new ArrayList<>();
            for (String line : item.lore) {
                lore.add(MiniMessageParser.parse(line));
            }

            return new ItemConfig(
                itemConfig,
                type, MiniMessageParser.parse(item.itemName), itemModel, lore, GearCooldowns.fromItemConfig(itemConfig)
            );
        }

        private void register() {
            itemConfigByItemModel.put(modelId, this);
            if (gearCooldowns != null) CooldownTracker.registerGearCooldowns(gearCooldowns);
        }
    }

    public static void loadFromResources(ResourceManager resourceManager) {
        long start = System.nanoTime();

        itemConfigByItemModel.clear();

        Yaml yaml = new Yaml(ItemConfigSchema.constructor(new LoaderOptions()));

        String itemConfigPath = "config/items";
        resourceManager.listResources(
            itemConfigPath,
            id -> id.getNamespace().equals(MiACompat.MOD_ID)
                && id.getPath().endsWith(".yml")
        ).forEach((id, resource) -> {
            try (InputStream is = resource.open()) {
                ItemConfigSchema itemConfig = yaml.load(is);

                Path relative = Path.of(itemConfigPath).relativize(
                    Path.of(id.getPath())
                );

                ItemConfig item = RelicConfig.isRelic(relative)
                    ? RelicConfig.parse(itemConfig)
                    : RelicConfig.tryParse(itemConfig);

                if (item == null) {
                    item = ItemConfig.parse(itemConfig);
                }

                item.register();
            } catch (Exception e) {
                MiACompat.LOGGER.error("Failed to load item config '{}'", id, e);
            }
        });

        long elapsed = System.nanoTime() - start;
        MiACompat.LOGGER.info(
            "Loaded {} Mine in Abyss item configs in {} ms",
            itemConfigByItemModel.size(),
            elapsed / 1_000_000.0
        );
    }
}
