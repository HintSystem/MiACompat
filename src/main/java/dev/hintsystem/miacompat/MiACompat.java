package dev.hintsystem.miacompat;

import dev.hintsystem.miacompat.config.Config;
import dev.hintsystem.miacompat.gui.Hud;
import dev.hintsystem.miacompat.mods.SupportIris;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MiACompat implements ClientModInitializer {
	public static final String MOD_ID = "miacompat";
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Config config = new Config();

    public static final GhostSeekTracker ghostSeekTracker = new GhostSeekTracker();
    private static final GhostSeekRenderer ghostSeekRenderer = new GhostSeekRenderer(ghostSeekTracker);

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

    public static boolean isMiAServer() {
        ServerData serverInfo = Minecraft.getInstance().getCurrentServer();
        return serverInfo != null && serverInfo.ip.contains("mineinabyss");
    }

	@Override
	public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("iris")) SupportIris.assignPipelines();

        config.loadFromFile();
        BonfireTracker.loadFromFile();

        Minecraft client = Minecraft.getInstance();

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            BonfireTracker.tick(c);
            ghostSeekTracker.tick(c);
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.HELD_ITEM_TOOLTIP, id("miacompat_hud"), new Hud());

        WorldRenderEvents.END_MAIN.register(this::onRenderWorld);

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() || !(entity instanceof Interaction interaction)) return InteractionResult.PASS;

            Display.ItemDisplay bonfire = BonfireTracker.findBonfire(
                world.getEntities(player, interaction.getBoundingBox().inflate(0.5))
            );
            if (bonfire != null) BonfireTracker.setTrackedBonfire(bonfire);

            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() || !(entity instanceof Interaction interaction)) return InteractionResult.PASS;
            if (!config.clearBreadcrumbsOnFind) return InteractionResult.PASS;

            for (Entity entityNear : world.getEntities(player, interaction.getBoundingBox().inflate(1.5))) {
                if (entityNear instanceof Display.ItemDisplay itemDisplay && GhostSeekTracker.isPrayingSkeleton(itemDisplay)) {
                    ghostSeekTracker.clearMeasurements();
                    break;
                }
            }

            return InteractionResult.PASS;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(ClientCommandManager.literal("miacompat")

                .then(ClientCommandManager.literal("config")
                    .executes(context -> {
                        client.schedule(() -> client.setScreen(config.createScreen(null)));
                        return 1;
                    }))

                .then(ClientCommandManager.literal("breadcrumbs")
                    .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("pingLength", IntegerArgumentType.integer(0, 5))
                            .executes(context -> {
                                int pingLength = IntegerArgumentType.getInteger(context, "pingLength");

                                GhostSeekTracker.GhostSeekType ghostSeekType = GhostSeekTracker.GhostSeekType.REFINED;
                                ghostSeekTracker.awaitingPingTicks = ghostSeekType.pingIntervalTicks;
                                ghostSeekTracker.addMeasurement(
                                    ghostSeekType.getPingMeasurement(client.player.position(), pingLength)
                                );
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("clear")
                        .executes(context -> {
                            ghostSeekTracker.clearMeasurements();
                            return 1;
                        })))

                .then(ClientCommandManager.literal("bonfire")
                    .executes(context -> {
                        var bonfire = BonfireTracker.bonfireData;

                        if (bonfire.lastSetTimestamp == 0) {
                            context.getSource().sendFeedback(
                                Component.literal("No bonfire has been tracked yet. Link yourself to a bonfire again to start tracking.")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                            );
                            return 1;
                        }

                        Instant instant = Instant.ofEpochMilli(bonfire.lastSetTimestamp);
                        ZonedDateTime localTime = instant.atZone(ZoneId.systemDefault());

                        Duration duration = Duration.between(localTime, ZonedDateTime.now());
                        String timeDisplay;

                        if (duration.toDays() < 7) {
                            long days = duration.toDays();
                            long hours = duration.toHours() % 24;
                            long minutes = duration.toMinutes() % 60;

                            StringBuilder sb = new StringBuilder();
                            if (days > 0) sb.append(days).append(days == 1 ? " day" : " days");
                            if (hours > 0) {
                                if (!sb.isEmpty()) sb.append(", ");
                                sb.append(hours).append(hours == 1 ? " hour" : " hours");
                            }
                            if (days == 0 && hours == 0 && minutes > 0) {
                                if (!sb.isEmpty()) sb.append(", ");
                                sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
                            }

                            timeDisplay = sb.isEmpty() ? "just now" : sb + " ago";
                        } else {
                            timeDisplay = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").format(localTime);
                        }

                        String pos = String.format("(%d, %d, %d)", bonfire.x, bonfire.y, bonfire.z);

                        MutableComponent bonfireSetMessage = bonfire.isBonfireSet
                            ? Component.literal("You have a linked bonfire\n").setStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.GREEN))
                            : Component.literal("You have no linked bonfire\n").setStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.RED));

                        MutableComponent feedback = Component.empty().append(bonfireSetMessage)
                            .append(Component.literal("Last position: ").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)))
                            .append(Component.literal(pos).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
                            .append(Component.literal("\nLast linked: ").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)))
                            .append(Component.literal(timeDisplay).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));

                        context.getSource().sendFeedback(feedback);
                        return 1;
                    }))
            );

        });
	}

    private void onRenderWorld(WorldRenderContext context) {
        ghostSeekRenderer.render(context);
    }

    public static void close() {
        ghostSeekRenderer.close();
    }
}