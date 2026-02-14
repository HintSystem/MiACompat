package dev.hintsystem.miacompat.config;

import dev.hintsystem.miacompat.GhostSeekRenderer;
import dev.hintsystem.miacompat.MiACompat;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class Config {
    private static final Path SAVE_PATH = MiACompat.CONFIG_DIR.resolve(MiACompat.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Color.class, new ColorTypeAdapter())
        .create();

    public static final Config DEFAULTS = new Config();

    public int maxWaypointRadius = 0;
    public boolean showBonfireWaypoint = true;
    public boolean showCurseMeter = true;
    public boolean showGhostSeekCooldown = true;
    public boolean ghostSeekDistanceHint = true;
    public boolean clearBreadcrumbsOnFind = true;
    public int breadcrumbDuration = 300;
    public GhostSeekRenderer.BreadcrumbRenderType breadcrumbRenderType = GhostSeekRenderer.BreadcrumbRenderType.FILLED_BOX;
    public float breadcrumbLineWidth = 8f;
    public float breadcrumbSize = 0.8f;
    public double breadcrumbDistanceScale = 0.5f;
    public double breadcrumbOpacity = 0.75f;
    public boolean showBreadcrumbsOnMap = false;
    public boolean pingColorMatchesBreadcrumb = true;
    public List<Color> breadcrumbColors = List.of(
        Color.decode("#A2453F"),
        Color.decode("#5B62A5"),
        Color.decode("#F3CB2D"),
        Color.decode("#65C756"),
        Color.decode("#17D8C5")
    );

    public Screen createScreen(Screen parent) {
        Option<Float> breadcrumbLineWidthOption = Option.<Float>createBuilder()
            .name(Component.literal("Breadcrumb Line Width"))
            .binding(DEFAULTS.breadcrumbLineWidth, () -> breadcrumbLineWidth, val -> breadcrumbLineWidth = val)
            .controller(opt -> FloatFieldControllerBuilder.create(opt)
                .range(2f, 50f))
            .build();

        OptionGroup.Builder breadcrumbColorsGroup = OptionGroup.createBuilder()
            .name(Component.literal("Ghost Seek Breadcrumb Colors"));

        breadcrumbColorsGroup.option(Option.<Boolean>createBuilder()
            .name(Component.literal("Match With Action Bar Pings"))
            .description(OptionDescription.of(Component.literal(
                """
                If enabled, the action bar message you get after a ghost seek ping will be edited so its color matches the breadcrumb colors
                """
            )))
            .binding(DEFAULTS.pingColorMatchesBreadcrumb, () -> pingColorMatchesBreadcrumb, val -> pingColorMatchesBreadcrumb = val)
            .controller(TickBoxControllerBuilder::create)
            .build());

        for (int i = 0; i < DEFAULTS.breadcrumbColors.size(); i++) {
            final int index = i; // Capture for lambda
            breadcrumbColorsGroup.option(Option.<Color>createBuilder()
                .name(Component.literal("Ping " + (i + 1)))
                .binding(DEFAULTS.breadcrumbColors.get(index), () -> breadcrumbColors.get(index), val -> {
                    List<Color> newList = new java.util.ArrayList<>(breadcrumbColors);
                    newList.set(index, val);
                    breadcrumbColors = newList;
                })
                .controller(ColorControllerBuilder::create)
                .build());
        }

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("PlayerRelayClient Config"))

            .category(ConfigCategory.createBuilder()
                .name(Component.literal("General"))

                .option(Option.<Integer>createBuilder()
                    .name(Component.literal("Max Waypoint Distance"))
                    .description(OptionDescription.of(Component.literal(
                        """
                        Defines the maximum distance (in meters) at which waypoints are visible.
                        
                        Unlike Xaero’s Minimap "Max WP Render Distance" setting, this limit also considers the waypoint’s
                        vertical distance from the player.
                    
                        Set to 0 to display all waypoints.
                        """
                    )))
                    .binding(DEFAULTS.maxWaypointRadius, () -> maxWaypointRadius, val -> maxWaypointRadius = val)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .formatValue(val -> Component.literal(String.format("%dm", val)))
                        .step(100)
                        .range(0, 10_000))
                    .build())

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show Bonfire Waypoint"))
                    .binding(DEFAULTS.showBonfireWaypoint, () -> showBonfireWaypoint, val -> showBonfireWaypoint = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show Curse Meter"))
                    .binding(DEFAULTS.showCurseMeter, () -> showCurseMeter, val -> showCurseMeter = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .group(OptionGroup.createBuilder()
                    .name(Component.literal("Ghost Seek"))

                    .option(Option.<Boolean>createBuilder()
                        .name(Component.literal("Show Cooldown Bar"))
                        .binding(DEFAULTS.showGhostSeekCooldown, () -> showGhostSeekCooldown, val -> showGhostSeekCooldown = val)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.<Boolean>createBuilder()
                        .name(Component.literal("Ping Distance Hints"))
                        .description(OptionDescription.of(Component.literal(
                            """
                            If enabled, displays the approximate distance from a praying skeleton in the action bar when you get a ghost seek ping
                            
                            Example:
                            dum tick (100-150 blocks)
                            """
                        )))
                        .binding(DEFAULTS.ghostSeekDistanceHint, () -> ghostSeekDistanceHint, val -> ghostSeekDistanceHint = val)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.<Boolean>createBuilder()
                        .name(Component.literal("Clear Breadcrumbs On Find"))
                        .description(OptionDescription.of(Component.literal(
                            """
                            If enabled, breadcrumbs will be cleared when hitting a praying skeleton
                            """
                        )))
                        .binding(DEFAULTS.clearBreadcrumbsOnFind, () -> clearBreadcrumbsOnFind, val -> clearBreadcrumbsOnFind = val)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .option(Option.<Integer>createBuilder()
                        .name(Component.literal("Breadcrumb Duration"))
                        .description(OptionDescription.of(Component.literal(
                            """
                            How long ghost seek breadcrumbs remain visible before disappearing.
                            
                            Set to 0 to disable ghost seek breadcrumbs.
                            """
                        )))
                        .binding(DEFAULTS.breadcrumbDuration, () -> breadcrumbDuration, val -> breadcrumbDuration = val)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .formatValue(val -> Component.literal(String.format("%ds", val)))
                            .step(5)
                            .range(0, 3_600))
                        .build())

                    .option(Option.<GhostSeekRenderer.BreadcrumbRenderType>createBuilder()
                        .name(Component.literal("Breadcrumb Visual Type"))
                        .description(OptionDescription.of(Component.literal(
                            """
                            Adjusts how breadcrumbs are rendered.
                            """
                        )))
                        .addListener((option, event) -> {
                            breadcrumbLineWidthOption.setAvailable(option.pendingValue() == GhostSeekRenderer.BreadcrumbRenderType.WIREFRAME_BOX);
                        })
                        .binding(DEFAULTS.breadcrumbRenderType, () -> breadcrumbRenderType, val -> breadcrumbRenderType = val)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(GhostSeekRenderer.BreadcrumbRenderType.class))
                        .build())

                    .option(breadcrumbLineWidthOption)

                    .option(Option.<Float>createBuilder()
                        .name(Component.literal("Breadcrumb Size"))
                        .binding(DEFAULTS.breadcrumbSize, () -> breadcrumbSize, val -> breadcrumbSize = val)
                        .controller(opt -> FloatFieldControllerBuilder.create(opt)
                            .range(0.1f, 10f))
                        .build())

                    .option(Option.<Double>createBuilder()
                        .name(Component.literal("Breadcrumb Distance Scale"))
                        .description(OptionDescription.of(Component.literal(
                            """
                            Scales breadcrumb size based on distance to the praying skeleton.
                            
                            0 = no scaling
                            + = bigger when further away
                            - = bigger when closer
                            """
                        )))
                        .binding(DEFAULTS.breadcrumbDistanceScale, () -> breadcrumbDistanceScale, val -> breadcrumbDistanceScale = val)
                        .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                            .range(-2d, 2d)
                            .step(0.05d))
                        .build())

                    .option(Option.<Double>createBuilder()
                        .name(Component.literal("Breadcrumb Opacity"))
                        .binding(DEFAULTS.breadcrumbOpacity, () -> breadcrumbOpacity, val -> breadcrumbOpacity = val)
                        .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                            .range(0d, 1d)
                            .step(0.05d))
                        .build())

                    .option(Option.<Boolean>createBuilder()
                        .name(Component.literal("Breadcrumbs On World Map"))
                        .binding(DEFAULTS.showBreadcrumbsOnMap, () -> showBreadcrumbsOnMap, val -> showBreadcrumbsOnMap = val)
                        .controller(TickBoxControllerBuilder::create)
                        .build())

                    .build())

                .group(breadcrumbColorsGroup.build())

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

    public void loadBreadcrumbColors(Field f, List<Color> loaded) throws IllegalAccessException {
        if (loaded != null) {
            int size = DEFAULTS.breadcrumbColors.size();

            // Pad with defaults if too few
            while (loaded.size() < size) {
                loaded.add(DEFAULTS.breadcrumbColors.get(loaded.size()));
            }
            // Truncate if too many
            if (loaded.size() > size) {
                loaded = loaded.subList(0, size);
            }

            f.set(this, loaded);
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
                    if (f.getName().equals("breadcrumbColors")) {
                        loadBreadcrumbColors(
                            f, GSON.fromJson(root.get(f.getName()), new TypeToken<List<Color>>(){}.getType())
                        );
                    } else {
                        Object val = GSON.fromJson(root.get(f.getName()), f.getType());
                        f.set(this, val);
                    }

                }
            }
        } catch (Exception e) {
            MiACompat.LOGGER.error("Failed to deserialize config from {}", SAVE_PATH, e);
        }
    }
}
