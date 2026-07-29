package dev.hintsystem.miacompat.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DebugTrigger {
    private static boolean prevDebugKeyDown = false;
    private static boolean debugKeyDown = false;

    public static Entity lastRaycastEntity;
    public static Set<UUID> lastRaycastVisitedEntityIds = new HashSet<>();

    public static void checkDebugKeys(Minecraft client) {
        if (client.level == null || client.player == null) return;
        debugKeyDown = false;

        if (debugKeyDown(client, InputConstants.KEY_V)) {

            EntityHitResult raycast = EntityDebug.raycast(client.player, 40);
            if (raycast == null) {
                PacketDebug.filterEntityPackets(Collections.emptyList());
                return;
            }

            lastRaycastEntity = raycast.getEntity();
            lastRaycastVisitedEntityIds = EntityDebug.logEntityInfo(client.level, lastRaycastEntity).visited();
            PacketDebug.filterEntityPackets(lastRaycastVisitedEntityIds);

        } else if (debugKeyDown(client, InputConstants.KEY_B)) {

            if (lastRaycastEntity == null) return;
            EntityDebug.logEntityInfo(client.level, lastRaycastEntity);

        }

        prevDebugKeyDown = debugKeyDown;
    }

    private static boolean debugKeyDown(Minecraft client, int key) {
        if (debugKeyDown) return false;

        if (InputConstants.isKeyDown(client.getWindow(), key)) {
            debugKeyDown = true;
            return !prevDebugKeyDown;
        }

        return false;
    }
}
