package dev.hintsystem.miacompat.client.screens;

import dev.hintsystem.miacompat.client.GhostSeekRenderer;
import dev.hintsystem.miacompat.config.Config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.List;

public class ConfigScreen {
    public static Screen create(Screen parent, Config config) {
        // General

        ConfigCategory generalCategory = ConfigCategory.createBuilder()
            .name(Component.literal("General"))

            .option(Option.<Integer>createBuilder()
                .name(Component.literal("Max Waypoint Distance"))
                .description(OptionDescription.of(Component.literal(
                    """
                    Defines the maximum distance (in meters) at which waypoints are visible
                    
                    Unlike Xaero’s Minimap "Max WP Render Distance" setting, this limit also considers the waypoint’s
                    vertical distance from the player
                
                    Set to 0 to display all waypoints
                    """
                )))
                .binding(Config.DEFAULTS.maxWaypointRadius, () -> config.maxWaypointRadius, val -> config.maxWaypointRadius = val)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                    .formatValue(val -> Component.literal(String.format("%dm", val)))
                    .step(100)
                    .range(0, 10_000))
                .build())

            .option(Option.<Boolean>createBuilder()
                .name(Component.literal("Show Bonfire Waypoint"))
                .binding(Config.DEFAULTS.showBonfireWaypoint, () -> config.showBonfireWaypoint, val -> config.showBonfireWaypoint = val)
                .controller(TickBoxControllerBuilder::create)
                .build())

            .option(Option.<Boolean>createBuilder()
                .name(Component.literal("Show Curse Meter"))
                .binding(Config.DEFAULTS.showCurseMeter, () -> config.showCurseMeter, val -> config.showCurseMeter = val)
                .controller(TickBoxControllerBuilder::create)
                .build())

            .option(Option.<Boolean>createBuilder()
                .name(Component.literal("Show Item Lore in Bundles"))
                .binding(Config.DEFAULTS.showItemLoreInBundles, () -> config.showItemLoreInBundles, val -> config.showItemLoreInBundles = val)
                .controller(TickBoxControllerBuilder::create)
                .build())


            .group(OptionGroup.createBuilder()
                .name(Component.literal("Orth Coins"))

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show Precise Coin Worth"))
                    .description(OptionDescription.of(Component.literal(
                        """
                        Additionally displays the exact Orth coin value with decimals
                        
                        Useful for tracking partial coin values when you don't have enough items to complete a full trade
                        """
                    )))
                    .binding(Config.DEFAULTS.showPreciseCoinWorth, () -> config.showPreciseCoinWorth, val -> config.showPreciseCoinWorth = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show Coin Worth in Containers"))
                    .description(OptionDescription.of(Component.literal(
                        """
                        Shows the total Orth coin value in chest and shulker box containers
                        
                        This displays how many whole coins you'd get by selling all items inside the container at the current trade rates
                        """
                    )))

                    .binding(Config.DEFAULTS.showCoinWorthInContainers, () -> config.showCoinWorthInContainers, val -> config.showCoinWorthInContainers = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show Coin Worth in Tooltips"))
                    .description(OptionDescription.of(Component.literal(
                        """
                        Shows the total Orth coin value in bundle and shulker box tooltips
                        
                        This displays how many whole coins you'd get by selling all items inside the container at the current trade rates
                        """
                    )))

                    .binding(Config.DEFAULTS.showCoinWorthInTooltips, () -> config.showCoinWorthInTooltips, val -> config.showCoinWorthInTooltips = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .build())


            .group(OptionGroup.createBuilder()
                .name(Component.literal("Gear Abilities"))

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show Cooldowns in Item Slots"))
                    .binding(Config.DEFAULTS.showGearCooldownsInItemSlots, () -> config.showGearCooldownsInItemSlots, val -> config.showGearCooldownsInItemSlots = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Hide Cooldowns in Action Bar"))
                    .binding(Config.DEFAULTS.hideGearCooldownsInActionBar, () -> config.hideGearCooldownsInActionBar, val -> config.hideGearCooldownsInActionBar = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Hide Ability Fails in Action Bar"))
                    .description(OptionDescription.of(Component.literal(
                            """
                            Hides the action bar message that appears when a gear ability fails
                            
                            Examples:
                            """)
                        .append(Component.literal(
                            """
                            Out of Food
                            Out of Experience / Out of Charge
                            Lacks Charge
                            """).withStyle(ChatFormatting.RED))
                    ))
                    .binding(Config.DEFAULTS.hideAbilityFailsInActionBar, () -> config.hideAbilityFailsInActionBar, val -> config.hideAbilityFailsInActionBar = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .build())


            .group(OptionGroup.createBuilder()
                .name(Component.literal("Compendium"))
                .collapsed(true)

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show Undiscovered Relics"))
                    .binding(Config.DEFAULTS.showUndiscoveredRelics, () -> config.showUndiscoveredRelics, val -> config.showUndiscoveredRelics = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .build())


            .build();

        // Ghost Seek

        Option<Float> breadcrumbLineWidthOption = Option.<Float>createBuilder()
            .name(Component.literal("Breadcrumb Line Width"))
            .binding(Config.DEFAULTS.breadcrumbLineWidth, () -> config.breadcrumbLineWidth, val -> config.breadcrumbLineWidth = val)
            .controller(opt -> FloatFieldControllerBuilder.create(opt)
                .range(2f, 50f))
            .build();

        OptionGroup.Builder breadcrumbColorsGroup = OptionGroup.createBuilder()
            .name(Component.literal("Breadcrumb Colors"))
            .option(Option.<Boolean>createBuilder()
                .name(Component.literal("Match With Action Bar Pings"))
                .description(OptionDescription.of(Component.literal(
                    """
                    If enabled, the action bar message you get after a ghost seek ping will be edited so its color matches the breadcrumb colors
                    """
                )))
                .binding(Config.DEFAULTS.pingColorMatchesBreadcrumb, () -> config.pingColorMatchesBreadcrumb, val -> config.pingColorMatchesBreadcrumb = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        for (int i = 0; i < Config.DEFAULTS.breadcrumbColors.size(); i++) {
            final int index = i; // Capture for lambda
            breadcrumbColorsGroup.option(Option.<Color>createBuilder()
                .name(Component.literal("Ping " + (i + 1)))
                .binding(Config.DEFAULTS.breadcrumbColors.get(index), () -> config.breadcrumbColors.get(index), val -> {
                    List<Color> newList = new java.util.ArrayList<>(config.breadcrumbColors);
                    newList.set(index, val);
                    config.breadcrumbColors = newList;
                })
                .controller(ColorControllerBuilder::create)
                .build());
        }

        ConfigCategory ghostSeekCategory = ConfigCategory.createBuilder()
            .name(Component.literal("Ghost Seek"))

            .option(Option.<Boolean>createBuilder()
                .name(Component.literal("Show Cooldown Bar"))
                .binding(Config.DEFAULTS.showGhostSeekCooldown, () -> config.showGhostSeekCooldown, val -> config.showGhostSeekCooldown = val)
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
                .binding(Config.DEFAULTS.ghostSeekDistanceHint, () -> config.ghostSeekDistanceHint, val -> config.ghostSeekDistanceHint = val)
                .controller(TickBoxControllerBuilder::create)
                .build())

            .option(Option.<Boolean>createBuilder()
                .name(Component.literal("Clear Breadcrumbs On Find"))
                .description(OptionDescription.of(Component.literal(
                    """
                    If enabled, breadcrumbs will be cleared after hitting a praying skeleton
                    """
                )))
                .binding(Config.DEFAULTS.clearBreadcrumbsOnFind, () -> config.clearBreadcrumbsOnFind, val -> config.clearBreadcrumbsOnFind = val)
                .controller(TickBoxControllerBuilder::create)
                .build())


            .group(OptionGroup.createBuilder()
                .name(Component.literal("Breadcrumb Visuals"))

                .option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Breadcrumbs On World Map"))
                    .binding(Config.DEFAULTS.showBreadcrumbsOnMap, () -> config.showBreadcrumbsOnMap, val -> config.showBreadcrumbsOnMap = val)
                    .controller(TickBoxControllerBuilder::create)
                    .build())

                .option(Option.<Integer>createBuilder()
                    .name(Component.literal("Breadcrumb Duration"))
                    .description(OptionDescription.of(Component.literal(
                        """
                        How long ghost seek breadcrumbs remain visible before disappearing
                        
                        Set to 0 to disable ghost seek breadcrumbs
                        """
                    )))
                    .binding(Config.DEFAULTS.breadcrumbDuration, () -> config.breadcrumbDuration, val -> config.breadcrumbDuration = val)
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .formatValue(val -> Component.literal(String.format("%ds", val)))
                        .step(5)
                        .range(0, 3_600))
                    .build())

                .option(Option.<GhostSeekRenderer.BreadcrumbRenderType>createBuilder()
                    .name(Component.literal("Breadcrumb Visual Type"))
                    .description(OptionDescription.of(Component.literal("Adjusts how breadcrumbs are rendered ")))
                    .addListener((option, event) -> {
                        breadcrumbLineWidthOption.setAvailable(option.pendingValue() == GhostSeekRenderer.BreadcrumbRenderType.WIREFRAME_BOX);
                    })
                    .binding(Config.DEFAULTS.breadcrumbRenderType, () -> config.breadcrumbRenderType, val -> config.breadcrumbRenderType = val)
                    .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(GhostSeekRenderer.BreadcrumbRenderType.class))
                    .build())

                .option(breadcrumbLineWidthOption)

                .option(Option.<Float>createBuilder()
                    .name(Component.literal("Breadcrumb Size"))
                    .binding(Config.DEFAULTS.breadcrumbSize, () -> config.breadcrumbSize, val -> config.breadcrumbSize = val)
                    .controller(opt -> FloatFieldControllerBuilder.create(opt)
                        .range(0.1f, 10f))
                    .build())

                .option(Option.<Double>createBuilder()
                    .name(Component.literal("Breadcrumb Distance Scale"))
                    .description(OptionDescription.of(Component.literal(
                        """
                        Scales breadcrumb size based on distance to the praying skeleton
                        
                        0 = no scaling
                        + = bigger when further away
                        - = bigger when closer
                        """
                    )))
                    .binding(Config.DEFAULTS.breadcrumbDistanceScale, () -> config.breadcrumbDistanceScale, val -> config.breadcrumbDistanceScale = val)
                    .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                        .range(-2d, 2d)
                        .step(0.05d))
                    .build())

                .option(Option.<Double>createBuilder()
                    .name(Component.literal("Breadcrumb Opacity"))
                    .binding(Config.DEFAULTS.breadcrumbOpacity, () -> config.breadcrumbOpacity, val -> config.breadcrumbOpacity = val)
                    .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                        .range(0d, 1d)
                        .step(0.05d))
                    .build())

                .build())


            .group(breadcrumbColorsGroup.build())


            .build();

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("PlayerRelayClient Config"))

            .category(generalCategory)
            .category(ghostSeekCategory)

            .save(config::saveToFile)
            .build()
            .generateScreen(parent);
    }
}
