package dev.hintsystem.miacompat.gui.screens.compendium.mobs;

import dev.hintsystem.miacompat.MiACompat;
import dev.hintsystem.miacompat.server.ServerLayerRegistry;
import dev.hintsystem.miacompat.server.config.LayerConfig;
import dev.hintsystem.miacompat.server.config.LayerMeta;
import dev.hintsystem.miacompat.server.config.geary.SpawnConfig;
import dev.hintsystem.miacompat.server.config.mythic.mob.MobConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class MobSlot {
    public static final int SLOT_SIZE = 48;

    public final String mobId;
    public final Component name;
    public final Identifier sprite;

    public final List<SpawnConfig> spawns = new ArrayList<>();
    public final Set<LayerMeta> layerMetas = EnumSet.noneOf(LayerMeta.class);

    public MobSlot(MobConfig mob) {
        this.mobId = mob.id;
        this.name = mob.display;
        this.sprite = MiACompat.id("mineinabyss/mobs/" + mob.id);
    }

    public void addSpawn(SpawnConfig spawn) {
        spawns.add(spawn);

        for (String region : spawn.regions) {
            LayerConfig layer = ServerLayerRegistry.getLayer(region);
            if (layer != null) layerMetas.add(layer.meta);
        }
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite,
            SLOT_SIZE, SLOT_SIZE, 0, 0, x, y, SLOT_SIZE, SLOT_SIZE);
    }
}
