package dev.hintsystem.miacompat;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Config {
    private static final Path SAVE_PATH = MiACompat.CONFIG_DIR.resolve(MiACompat.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public static final Config DEFAULTS = new Config();

    public int maxWaypointRadius = 0;
    public boolean showBonfireWaypoint = true;
    public boolean ghostSeekDistanceHint = true;
    public int ghostSeekBreadcrumbDuration = 300;

    public Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("PlayerRelayClient Config"))

            .category(ConfigCategory.createBuilder()
                .name(Text.literal("General"))

                .option(Option.<Integer>createBuilder()
                    .name(Text.literal("Max Waypoint Distance"))
                    .description(OptionDescription.of(Text.literal(
                        """
                        Defines the maximum distance (in meters) at which waypoints are visible.
                        
                        Unlike Xaero’s Minimap "Max WP Render Distance" setting, this limit also considers the waypoint’s
                        vertical distance from the player.
                    
                        Set to 0 to display all waypoints.
                        """
                    )))
                    .binding(DEFAULTS.maxWaypointRadius, () -> maxWaypointRadius, val -> maxWaypointRadius = val)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .formatValue(val -> Text.literal(String.format("%dm", val)))
                        .step(100)
                        .range(0, 10_000))
                    .build())

                .option(Option.<Boolean>createBuilder()
                    .name(Text.literal("Show Bonfire Waypoint"))
                    .binding(DEFAULTS.showBonfireWaypoint, () -> showBonfireWaypoint, val -> showBonfireWaypoint = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Text.literal("Ghost Seek"))

                    .option(Option.<Boolean>createBuilder()
                        .name(Text.literal("Distance Hints"))
                        .description(OptionDescription.of(Text.literal(
                            """
                            If enabled, displays the approximate distance from a praying skeleton in the action bar when you get a ghost seek ping
                            
                            Example:
                            dum tick (100-150 blocks)
                            """
                        )))
                        .binding(DEFAULTS.ghostSeekDistanceHint, () -> ghostSeekDistanceHint, val -> ghostSeekDistanceHint = val)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.<Integer>createBuilder()
                        .name(Text.literal("Breadcrumb Duration"))
                        .description(OptionDescription.of(Text.literal(
                            """
                            How long ghost seek breadcrumbs remain visible before disappearing.
                            
                            Set to 0 to disable ghost seek breadcrumbs.
                            """
                        )))
                        .binding(DEFAULTS.ghostSeekBreadcrumbDuration, () -> ghostSeekBreadcrumbDuration, val -> ghostSeekBreadcrumbDuration = val)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .formatValue(val -> Text.literal(String.format("%ds", val)))
                            .step(5)
                            .range(0, 3_600))
                        .build())

                    .build())

                .build())

            .save(this::saveToFile)
            .build()
            .generateScreen(parent);
    }

    public void saveToFile() {
        JsonObject root = new JsonObject();

        try {
            for (Field f : Config.class.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    Object current = f.get(this);
                    Object def = f.get(DEFAULTS);

                    if (!Objects.equals(current, def)) {
                        root.add(f.getName(), GSON.toJsonTree(current));
                    }
                }
            }

            Files.writeString(SAVE_PATH, GSON.toJson(root));
        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to serialize config at {}", SAVE_PATH, e);
        }
    }

    public void loadFromFile() {
        if (!Files.exists(SAVE_PATH)) {
            MiACompat.LOGGER.info("Config file not found at {}, using default", SAVE_PATH);
            saveToFile();
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(Files.readString(SAVE_PATH)).getAsJsonObject();

            for (Field f : this.getClass().getFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && root.has(f.getName())) {
                    Object val = GSON.fromJson(root.get(f.getName()), f.getType());
                    f.set(this, val);
                }
            }
        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to deserialize config from {}", SAVE_PATH, e);
        }
    }
}
