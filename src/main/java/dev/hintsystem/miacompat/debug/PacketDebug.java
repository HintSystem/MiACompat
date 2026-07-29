package dev.hintsystem.miacompat.debug;

import dev.hintsystem.miacompat.MiACompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PacketDebug {
    public static Set<UUID> allowedPacketEntityIds = new HashSet<>();

    public static void handlePacket(Packet<?> packet) {
        if (packet instanceof ClientboundAddEntityPacket) return;
        if (packet instanceof ClientboundRemoveEntitiesPacket) return;

        Minecraft client = Minecraft.getInstance();

        if (client.isSameThread()) {
            logPacket(packet);
        } else {
            client.execute(() -> logPacket(packet));
        }
    }

    public static void handleAddEntityPacket(ClientboundAddEntityPacket packet) {
        logEntityPacket(packet);
    }

    public static void handleRemoveEntitiesPacket(ClientboundRemoveEntitiesPacket packet) {
        logEntityPacket(packet);
    }

    public static void filterEntityPackets(Collection<UUID> allowedEntityIds) {
        allowedPacketEntityIds.clear();
        allowedPacketEntityIds.addAll(allowedEntityIds);
    }

    public static boolean isEntityIdAllowed(UUID entityId) {
        return allowedPacketEntityIds.isEmpty() || allowedPacketEntityIds.contains(entityId);
    }

    private static void logPacket(Packet<?> packet) {
        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            for (Packet<?> subPacket : bundlePacket.subPackets()) {
                logPacket(subPacket);
            }

            return;
        }

        if (Set.of(
            GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE,
            GamePacketTypes.CLIENTBOUND_BOSS_EVENT,
            GamePacketTypes.CLIENTBOUND_LEVEL_PARTICLES,
            GamePacketTypes.CLIENTBOUND_SET_TIME,
            GamePacketTypes.CLIENTBOUND_LEVEL_CHUNK_WITH_LIGHT,
            GamePacketTypes.CLIENTBOUND_LIGHT_UPDATE,
            CommonPacketTypes.CLIENTBOUND_KEEP_ALIVE,
            CommonPacketTypes.CLIENTBOUND_PING
        ).contains(packet.type())) return;

        if (logEntityPacket(packet)) return;

        //MiACompat.LOGGER.info("p: {}, c: {}", packet.type().id(), packet.getClass());
    }

    public static boolean logEntityPacket(Packet<?> packet) {
        String entityPacketInfo = describeEntityPacket(packet);
        if (entityPacketInfo == null) return false;

        MiACompat.LOGGER.info(entityPacketInfo);
        return true;
    }

    @Nullable
    public static String describeEntityPacket(Packet<?> packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;

        StringBuilder s = new StringBuilder();
        s.append("\npacket: ").append(packet.type().id());

        if (packet instanceof ClientboundRemoveEntitiesPacket entityPacket) {
            String packetDescription = describeRemovedEntitites(entityPacket, client.level);
            return packetDescription != null
                ? s.append(packetDescription).toString() : null;
        }

        int entityId = getPacketEntityId(packet);
        Entity entity = null;

        if (entityId != -1) {
            entity = client.level.getEntity(entityId);
        } else {
            if (packet instanceof ClientboundMoveEntityPacket entityPacket) {
                entity = entityPacket.getEntity(client.level);
            } else if (packet instanceof ClientboundRotateHeadPacket entityPacket) {
                entity = entityPacket.getEntity(client.level);
            } else if (packet instanceof ClientboundEntityEventPacket entityPacket) {
                entity = entityPacket.getEntity(client.level);
            }
        }

        if (entity != null) {
            String packetDescription = describeEntityPacket(entity, packet);
            if (packetDescription == null)
                return null;

            return s.append("\n")
                .append(describeEntityPacket(entity, packet)).toString();
        }

        if (entityId != -1 && allowedPacketEntityIds.isEmpty())
            return s.append('\n').append(EntityDebug.unknownEntityId(entityId)).toString();

        return null;
    }

    @Nullable
    private static String describeRemovedEntitites(ClientboundRemoveEntitiesPacket packet, Level level) {
        StringBuilder s = new StringBuilder();

        boolean hasAllowedEntity = false;
        for (int id : packet.getEntityIds()) {
            Entity removedEntity = level.getEntity(id);

            s.append("\n");
            if (removedEntity == null) {
                s.append(EntityDebug.unknownEntityId(id));
                continue;
            }

            if (!hasAllowedEntity)
                hasAllowedEntity = isEntityIdAllowed(removedEntity.getUUID());

            s.append(EntityDebug.describe(removedEntity));
        }

        if (!hasAllowedEntity) return null;

        return s.toString();
    }

    @Nullable
    private static String describeEntityPacket(@NotNull Entity entity, Packet<?> packet) {
        if (!isEntityIdAllowed(entity.getUUID())) return null;

        StringBuilder s = new StringBuilder();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.distanceToSqr(entity) > 400 * 400) {
            s.append("[").append(player.distanceTo(entity)).append("] ");
        }

        s.append("entity: ").append(EntityDebug.describe(entity));

        if (packet instanceof ClientboundSetEntityDataPacket entityDataPacket) {
            s.append("\ndata: ").append(EntityDebug.getEntityData(entity, entityDataPacket.packedItems()));
        }

        return s.toString();
    }

    public static int getPacketEntityId(Packet<?> packet) {
        if (packet instanceof ClientboundAddEntityPacket entityPacket) {
            return -1; // entityPacket.getId();
        } else if (packet instanceof ClientboundSetEntityMotionPacket entityPacket) {
            return entityPacket.getId();
        } else if (packet instanceof ClientboundEntityPositionSyncPacket entityPacket) {
            return entityPacket.id();
        } else if (packet instanceof ClientboundTeleportEntityPacket entityPacket) {
            return entityPacket.id();
        } else if (packet instanceof ClientboundSetEntityDataPacket entityPacket) {
            return entityPacket.id();
        } else if (packet instanceof ClientboundUpdateAttributesPacket entityPacket) {
            return entityPacket.getEntityId();
        } else if (packet instanceof ClientboundSetPassengersPacket entityPacket) {
            return entityPacket.getVehicle();
        } else if (packet instanceof ClientboundDamageEventPacket entityPacket) {
            return entityPacket.entityId();
        }

        return -1;
    }
}
