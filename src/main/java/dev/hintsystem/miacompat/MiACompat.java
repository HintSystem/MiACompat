package dev.hintsystem.miacompat;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.api.ClientModInitializer;

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

    public static boolean isMiAServer() {
        ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        return serverInfo != null && serverInfo.address.contains("mineinabyss");
    }

	@Override
	public void onInitializeClient() {
        config.loadFromFile();
        BonfireTracker.loadFromFile();

        MinecraftClient client = MinecraftClient.getInstance();

        ClientTickEvents.END_CLIENT_TICK.register((c) -> {
            BonfireTracker.tick();
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient || !(entity instanceof InteractionEntity interaction)) return ActionResult.PASS;

            DisplayEntity.ItemDisplayEntity bonfire = BonfireTracker.findBonfire(
                world.getOtherEntities(player, interaction.getBoundingBox().expand(0.5))
            );
            if (bonfire != null) BonfireTracker.setTrackedBonfire(bonfire);

            return ActionResult.PASS;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(ClientCommandManager.literal("miacompat")
                .then(ClientCommandManager.literal("config")
                    .executes(context -> {
                        client.send(() -> client.setScreen(config.createScreen(null)));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("bonfire")
                    .executes(context -> {
                        var bonfire = BonfireTracker.bonfireData;

                        if (bonfire.lastSetTimestamp == 0) {
                            context.getSource().sendFeedback(
                                Text.literal("No bonfire has been tracked yet. Link yourself to a bonfire again to start tracking.")
                                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW))
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

                        MutableText bonfireSetMessage = bonfire.isBonfireSet
                            ? Text.literal("You have a linked bonfire\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.GREEN))
                            : Text.literal("You have no linked bonfire\n").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.RED));

                        MutableText feedback = Text.empty().append(bonfireSetMessage)
                            .append(Text.literal("Last position: ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)))
                            .append(Text.literal(pos).setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                            .append(Text.literal("\nLast linked: ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)))
                            .append(Text.literal(timeDisplay).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

                        context.getSource().sendFeedback(feedback);
                        return 1;
                    }))
            );

        });
	}
}