package dev.hintsystem.miacompat.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MiaDeeperWorld {
    public static final int LAYER_HEIGHT = 480;
    public static final int X_SHIFT = 16384;

    public enum LayerInfo {
        Orth("Orth", 0, null, 0xFFA812, List.of()),
        L1("Edge of the Abyss", 0, 0, 0xDB4040, 0xBA1E1E, List.of(MobEffects.HUNGER)),
        L2("Forest of Temptation", 49152, -1368, 0x175ABF, 0x286FBF, List.of(MobEffects.POISON, MobEffects.HUNGER, MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE, MobEffects.WEAKNESS)),
        L3("Great Fault", 81920, -2424, 0xD428B1, 0xE82CC2, List.of(MobEffects.HUNGER, MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE, MobEffects.WEAKNESS)),
        L4("The Goblets of Giants", 131072, -3834, 0x557151, List.of(MobEffects.WITHER, MobEffects.HUNGER, MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE, MobEffects.WEAKNESS)),
        L5("Sea of Corpses", 196608, -5629, 0x424767, List.of(MobEffects.WITHER, MobEffects.HUNGER, MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE, MobEffects.WEAKNESS, MobEffects.DARKNESS));

        public final String title;
        public final int centerX;
        public final int startSection;
        public final Integer startY;
        public final int titleColor;
        public final Integer subtitleColor;
        public final List<Holder<@NotNull MobEffect>> ascensionEffects;

        LayerInfo(String title, int centerX, Integer startY, int titleColor, List<Holder<@NotNull MobEffect>> ascensionEffects) {
            this(title, centerX, startY, titleColor, null, ascensionEffects);
        }

        LayerInfo(String title, int centerX, Integer startY, int titleColor, Integer subtitleColor, List<Holder<@NotNull MobEffect>> ascensionEffects) {
            this.title = title;
            this.centerX = centerX;
            this.startSection = sectionFromX(centerX);
            this.startY = startY;
            this.titleColor = titleColor;
            this.subtitleColor = subtitleColor;
            this.ascensionEffects = ascensionEffects;
        }

        public int getSubSection(int section) {
            return Math.max(section - startSection + 1, 0);
        }

        public static LayerInfo fromUnwrappedY(double unwrappedY) {
            LayerInfo[] layers = values();
            for (int i = layers.length - 1; i >= 1; i--) {
                Integer startY = layers[i].startY;
                if (startY == null) continue;
                if (unwrappedY <= startY) {
                    return layers[i];
                }
            }

            return Orth;
        }
    }

    public static int sectionFromX(int x) {
        return Math.floorDiv(x - Math.floorDiv(X_SHIFT, 2), X_SHIFT) + 1;
    }

    public static double unwrapX(double x, int layer) {
        return x - layer * X_SHIFT;
    }

    public static double unwrapY(double y, int layer) {
        return y - layer * LAYER_HEIGHT;
    }

    public static BlockPos unwrap(BlockPos pos) {
        int layer = sectionFromX(pos.getX());

        return new BlockPos((int)unwrapX(pos.getX(), layer), (int)unwrapY(pos.getY(), layer), pos.getZ());
    }

    /**
     * Converts target coordinates to be relative to a reference point, accounting for layer wrapping.
     * <p>
     * This method calculates how many layers apart the target is from the reference, then
     * "unwraps" the target's coordinates by that layer delta. This produces coordinates
     * that are relative to the reference's layer, making them suitable for:
     * <ul>
     * <li>Rendering other waypoints on minimaps relative to the local player
     * <li>Any visualization that needs to show wrapped positions in a consistent coordinate space
     * </ul><p>
     *
     * Example:
     * <ul>
     * <li>Reference (player) is at layer 1 (section 1): x=100, y=50
     * <li>Target (other player) is at layer 2 (section 2): x=16484, y=530
     * <li>layerDelta = 2 - 1 = 1
     * <li>Result: target unwrapped by 1 layer = x=100, y=50 (appears at same relative position)
     * </ul>
     *
     * @param reference The reference position (typically the local player's position)
     * @param target The target position to relativize (e.g., another player or waypoint)
     * @return Target coordinates adjusted to be relative to the reference's layer
     */
    public static Vec3 relativizeWrapped(Vec3 reference, Vec3 target) {
        int layerDelta = sectionFromX((int)target.x()) - sectionFromX((int)reference.x());

        return new Vec3(
            unwrapX(target.x(), layerDelta),
            unwrapY(target.y(), layerDelta),
            target.z()
        );
    }
}
