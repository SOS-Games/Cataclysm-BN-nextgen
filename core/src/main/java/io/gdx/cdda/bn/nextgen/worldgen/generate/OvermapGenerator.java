package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingIndex;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Procedural mini-overmap layout: terrain, rivers, cities, roads (W4–W5). */
public final class OvermapGenerator {

    private OvermapGenerator() {}

    public static OvermapGenerateResult generate(
        final OvermapGenerateOptions options,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapConnectionRegistry connectionRegistry,
        final MutableSpecialRegistry mutableSpecials
    ) {
        return generate(
            options,
            buildings,
            oterRegistry,
            connectionRegistry,
            mutableSpecials,
            RegionSettingsRegistry.empty()
        );
    }

    public static OvermapGenerateResult generate(
        final OvermapGenerateOptions options,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapConnectionRegistry connectionRegistry,
        final MutableSpecialRegistry mutableSpecials,
        final RegionSettingsRegistry regionSettings
    ) {
        if (options == null) {
            throw new IllegalArgumentException("options is required");
        }
        final List<String> warnings = new ArrayList<>();
        final Random rng = new Random(options.getSeed() ^ 0x504C4143L);
        final RegionSettingsDefinition region = resolveRegion(options, regionSettings, warnings);
        final OvermapGrid grid = new OvermapGrid(
            options.getWidth(),
            options.getHeight(),
            options.getFieldId()
        );

        BaseTerrainFiller.fill(grid, options, region, oterRegistry, rng);

        int riversCarved;
        if (options.isLegacyGenerationOrder()) {
            riversCarved = RiverGenerator.carve(grid, options, oterRegistry, rng, warnings);
        } else {
            LakeGenerator.fill(grid, options, region, oterRegistry, rng, warnings);
            riversCarved = RiverGenerator.carve(grid, options, oterRegistry, rng, warnings);
            ThickForestGenerator.upgrade(grid, options, region, oterRegistry, warnings);
            SwampGenerator.fill(grid, options, region, oterRegistry, rng, warnings);
            BeachGenerator.paint(grid, options, region, oterRegistry, warnings);
        }

        final CityBuildingRegistry registry = buildings == null ? CityBuildingRegistry.empty() : buildings;
        final List<int[]> placedSites = new ArrayList<>();
        final List<PlacedBuildingRecord> placements = new ArrayList<>();

        final int regionSpecials = options.isLegacyGenerationOrder()
            ? 0
            : RegionSpecialPlacer.placeAll(
                grid,
                registry,
                oterRegistry,
                options,
                region,
                rng,
                warnings,
                placedSites,
                placements
            );

        final int cities = CityPlacer.placeAll(
            grid,
            registry,
            oterRegistry,
            options,
            region,
            rng,
            warnings,
            placedSites,
            placements
        );
        final int specials = regionSpecials + StaticSpecialPlacer.placeAll(
            grid,
            registry,
            oterRegistry,
            options,
            rng,
            warnings,
            placedSites,
            placements
        );
        final int mutablePlaced = MutableSpecialPlacer.placeAll(
            grid,
            mutableSpecials,
            oterRegistry,
            options,
            rng,
            warnings,
            placedSites,
            placements
        );

        final int roadCells = HighwayGenerator.connectSites(
            grid,
            placedSites,
            connectionRegistry,
            options,
            oterRegistry,
            rng,
            warnings
        );

        final PlacedBuildingIndex placementIndex = PlacedBuildingIndex.fromRecords(placements, warnings);

        return new OvermapGenerateResult(
            grid,
            warnings,
            cities,
            specials,
            mutablePlaced,
            riversCarved,
            roadCells,
            placementIndex
        );
    }

    private static RegionSettingsDefinition resolveRegion(
        final OvermapGenerateOptions options,
        final RegionSettingsRegistry regionSettings,
        final List<String> warnings
    ) {
        if (regionSettings == null || regionSettings.size() == 0) {
            return null;
        }
        final String regionId = options.getRegionId();
        final java.util.Optional<RegionSettingsDefinition> resolved = regionSettings.find(regionId);
        if (resolved.isPresent()) {
            return resolved.get();
        }
        final java.util.Optional<RegionSettingsDefinition> fallback = regionSettings.find("default");
        if (!"default".equals(regionId)) {
            addWarning(warnings, "unknown regionId '" + regionId + "'; using default region settings if present");
        }
        return fallback.orElse(null);
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
