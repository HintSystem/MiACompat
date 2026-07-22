package dev.hintsystem.miacompat.server;

import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.jetbrains.annotations.Nullable;

public class GearyData {
    // Sometimes data stores are retrieved every frame, this cache is here to prevent decoding prefabs each time
    private static final Map<CustomData, DataStore> DATA_STORE_CACHE = new WeakHashMap<>();

    @Nullable
    public static DataStore get(CustomData customDataComponent) {
        if (customDataComponent == null) return null;
        return DATA_STORE_CACHE.computeIfAbsent(customDataComponent, DataStore::new);
    }

    @Nullable
    public static DataStore get(ItemStack itemStack) {
        return get(itemStack.get(DataComponents.CUSTOM_DATA));
    }

    public static Set<Identifier> getPrefabIds(ItemStack item) {
        DataStore dataStore = get(item);
        return dataStore != null ? dataStore.getPrefabs() : Set.of();
    }

    @Nullable
    public static Identifier getFirstPrefabId(ItemStack item) {
        Set<Identifier> prefabIds = getPrefabIds(item);
        return !prefabIds.isEmpty() ? prefabIds.iterator().next() : null;
    }

    public static class DataStore {
        private static final CompoundTag EMPTY_TAG = new CompoundTag();

        public static final String COMPONENTS_KEY = "geary:components";
        public static final String PREFABS_KEY = "geary:prefabs";

        public final CompoundTag tag;
        private Set<Identifier> prefabs;

        public DataStore(CustomData customDataComponent) {
            this(customDataComponent.copyTag()
                .getCompound("PublicBukkitValues")
                .orElse(EMPTY_TAG));
        }

        public DataStore(CompoundTag tag) { this.tag = tag; }

        public Set<Identifier> getPrefabs() {
            if (this.prefabs == null) {
                this.prefabs = getPrefabsBytes()
                    .map(bytes -> Collections.unmodifiableSet(decodePrefabs(bytes)))
                    .orElseGet(Set::of);
            }

            return this.prefabs;
        }

        public Optional<byte[]> getPrefabsBytes() { return tag.getByteArray(PREFABS_KEY); }

        private Set<Identifier> decodePrefabs(byte[] bytes) {
            try {
                int i = 0;

                if ((bytes[i++] & 0xFF) != 0x9F) // major type 4, array prefix (0x80) + indefinite suffix (0x1F)
                    throw new IllegalArgumentException("Expected CBOR indefinite byte array");

                // preserve order
                Set<Identifier> prefabs = new LinkedHashSet<>();

                while (true) {
                    int head = bytes[i++] & 0xFF;

                    if (head == 0xFF) break;

                    // major type 3, utf-8 text string
                    if ((head & 0xE0) != 0x60) {
                        throw new IllegalArgumentException("Expected CBOR text string");
                    }

                    // read length argument
                    int arg = head & 0x1F;
                    Integer length = null;

                    if (arg < 24) length = arg;

                    if (arg == 24)
                        length = bytes[i++] & 0xFF;

                    if (arg == 25)
                        length = ((bytes[i++] & 0xFF) << 8)
                            | (bytes[i++] & 0xFF);

                    if (length == null)
                        throw new UnsupportedOperationException("Unsupported CBOR string length");

                    prefabs.add(Identifier.parse(
                        new String(bytes, i, length, StandardCharsets.UTF_8)
                    ));

                    i += length;
                }

                return prefabs;
            } catch (RuntimeException e) {
                MiACompat.LOGGER.warn("Failed to decode {} from bytes [{}]",
                    PREFABS_KEY, HexFormat.ofDelimiter(" ").formatHex(bytes), e);
                return Set.of();
            }
        }
    }
}
