package dev.hintsystem.miacompat.config;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.client.GhostSeekRenderer;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class Config extends PersistentData {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Color.class, new ColorTypeAdapter())
        .create();

    public static final Config DEFAULTS = new Config();

    // General
    public int maxWaypointRadius = 0;
    public boolean showBonfireWaypoint = true;
    public boolean showCurseMeter = true;
    public boolean showItemLoreInBundles = true;

    public boolean showPreciseCoinWorth = false;
    public boolean showCoinWorthInContainers = true;
    public boolean showCoinWorthInTooltips = true;

    public boolean showGearCooldownsInItemSlots = true;
    public boolean hideGearCooldownsInActionBar = false;
    public boolean hideAbilityFailsInActionBar = false;

    public boolean showUndiscoveredRelics = false;

    // Ghost Seek
    public boolean showGhostSeekCooldown = true;
    public boolean ghostSeekDistanceHint = true;
    public boolean clearBreadcrumbsOnFind = true;

    public boolean showBreadcrumbsOnMap = false;
    public int breadcrumbDuration = 300;
    public GhostSeekRenderer.BreadcrumbRenderType breadcrumbRenderType = GhostSeekRenderer.BreadcrumbRenderType.FILLED_BOX;
    public float breadcrumbLineWidth = 8f;
    public float breadcrumbSize = 0.8f;
    public double breadcrumbDistanceScale = 0.5f;
    public double breadcrumbOpacity = 0.75f;

    public boolean pingColorMatchesBreadcrumb = true;
    public List<Color> breadcrumbColors = List.of(
        Color.decode("#A2453F"),
        Color.decode("#5B62A5"),
        Color.decode("#F3CB2D"),
        Color.decode("#65C756"),
        Color.decode("#17D8C5")
    );

    @Override
    public String getDataTitle() { return "MiACompat settings"; }

    @Override
    public Path getFilePath() { return MiACompat.GLOBAL_CONFIG_DIR.resolve(MiACompat.MOD_ID + ".json"); }

    @Override
    public Path getBackupFolder() { return MiACompat.CONFIG_FOLDER; }

    @Override
    protected String serialize() throws IllegalAccessException {
        JsonObject root = new JsonObject();

        for (Field f : Config.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                Object current = f.get(this);
                Object def = f.get(DEFAULTS);

                if (!Objects.equals(current, def)) {
                    root.add(f.getName(), GSON.toJsonTree(current));
                }
            }
        }

        return GSON.toJson(root);
    }

    @Override
    protected void deserialize(String data) throws Exception {
        JsonObject root = GSON.fromJson(data, JsonObject.class);

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
    }

    private void loadBreadcrumbColors(Field f, List<Color> loaded) throws IllegalAccessException {
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
}
