package dev.hintsystem.miacompat.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class MiaWorldCoordinates {
    public static final int LAYER_HEIGHT = 480;
    public static final int X_SHIFT = 16384;

    public static int layerFromX(int x) {
        return Math.floorDiv(x - Math.floorDiv(X_SHIFT, 2), X_SHIFT) + 1;
    }

    public static double unwrapX(double x, int layer) {
        return x - layer * X_SHIFT;
    }

    public static double unwrapY(double y, int layer) {
        return y - layer * LAYER_HEIGHT;
    }

    public static Vec3 unwrap(Vec3 pos) {
        int layer = layerFromX((int)pos.x());

        return new Vec3(unwrapX(pos.x(), layer), unwrapY(pos.y(), layer), pos.z());
    }

    public static BlockPos unwrap(BlockPos pos) {
        int layer = layerFromX(pos.getX());

        return new BlockPos((int)unwrapX(pos.getX(), layer), (int)unwrapY(pos.getY(), layer), pos.getZ());
    }

    public static Vec3 relativizeWrapped(Vec3 reference, Vec3 target) {
        int layerDelta = layerFromX((int)target.x()) - layerFromX((int)reference.x());

        return new Vec3(
            unwrapX(target.x(), layerDelta),
            unwrapY(target.y(), layerDelta),
            target.z()
        );
    }
}
