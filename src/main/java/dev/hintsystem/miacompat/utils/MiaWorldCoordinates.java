package dev.hintsystem.miacompat.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MiaWorldCoordinates {
    private static final int LAYER_HEIGHT = 480;
    private static final int X_SHIFT = 16384;

    public static int layerFromX(int x) {
        return Math.floorDiv(x - Math.floorDiv(X_SHIFT, 2), X_SHIFT) + 1;
    }

    public static double unwrapX(double x, int layer) {
        return x - layer * X_SHIFT;
    }

    public static double unwrapY(double y, int layer) {
        return y - layer * LAYER_HEIGHT;
    }

    public static Vec3d unwrap(Vec3d pos) {
        int layer = layerFromX((int)pos.getX());

        return new Vec3d(unwrapX(pos.getX(), layer), unwrapY(pos.getY(), layer), pos.getZ());
    }

    public static BlockPos unwrap(BlockPos pos) {
        int layer = layerFromX(pos.getX());

        return new BlockPos((int)unwrapX(pos.getX(), layer), (int)unwrapY(pos.getY(), layer), pos.getZ());
    }

    public static Vec3d relativizeWrapped(Vec3d reference, Vec3d target) {
        int layerDelta = layerFromX((int)target.getX()) - layerFromX((int)reference.getX());

        return new Vec3d(
            unwrapX(target.getX(), layerDelta),
            unwrapY(target.getY(), layerDelta),
            target.getZ()
        );
    }
}
