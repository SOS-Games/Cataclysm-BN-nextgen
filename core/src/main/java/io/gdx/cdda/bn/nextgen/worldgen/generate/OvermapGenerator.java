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

            riversCarved += RiverGenerator.carve(
                grid,
                options,
                oterRegistry,
                new Random(options.getSeed() ^ RiverGenerator.SECOND_PASS_SEED_XOR),
                warnings
            );

            riversCarved -= RiverPolisher.smooth(grid, options, oterRegistry, warnings);

            ThickForestGenerator.upgrade(grid, options, region, oterRegistry, warnings);

            SwampGenerator.fill(grid, options, region, oterRegistry, rng, warnings);

            BeachGenerator.paint(grid, options, region, oterRegistry, warnings);

        }



        final CityBuildingRegistry registry = buildings == null ? CityBuildingRegistry.empty() : buildings;

        final List<int[]> placedSites = new ArrayList<>();

        final List<PlacedBuildingRecord> placements = new ArrayList<>();



        final CityGenerator.CityGenerateResult cityResult = CityGenerator.placeAll(

            grid,

            registry,

            oterRegistry,

            connectionRegistry,

            options,

            region,

            rng,

            warnings,

            placedSites,

            placements

        );

        final int cities = cityResult.totalCityPlacements();

        final int urbanOmtsPlaced = cityResult.getUrbanOmtsPlaced();

        final int localRoadCells = cityResult.getLocalRoadCellsPlaced();

        final List<UrbanSite> urbanSites = cityResult.getUrbanSites();



        int roadCells = 0;

        if (!options.isLegacyGenerationOrder()) {

            roadCells = paintInterCityHighways(

                grid,

                options,

                connectionRegistry,

                oterRegistry,

                rng,

                warnings,

                placedSites,

                urbanSites

            );

        }



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



        int forestTrailCells = 0;

        int undergroundCells = 0;

        if (!options.isLegacyGenerationOrder()) {

            forestTrailCells = ForestTrailGenerator.placeAll(

                grid,

                region,

                connectionRegistry,

                options,

                oterRegistry,

                rng,

                warnings

            );

            undergroundCells = UndergroundNetworkGenerator.placeAll(

                grid,

                urbanSites,

                region,

                connectionRegistry,

                options,

                oterRegistry,

                rng,

                warnings

            );

        }



        if (options.isLegacyGenerationOrder()) {

            roadCells = HighwayGenerator.connectSites(

                grid,

                placedSites,

                connectionRegistry,

                options,

                oterRegistry,

                rng,

                warnings

            );

        }



        final PlacedBuildingIndex placementIndex = PlacedBuildingIndex.fromRecords(placements, warnings);



        return new OvermapGenerateResult(

            grid,

            warnings,

            cities,

            specials,

            mutablePlaced,

            riversCarved,

            roadCells,

            placementIndex,

            urbanOmtsPlaced,

            localRoadCells,

            forestTrailCells,

            undergroundCells

        );

    }



    private static int paintInterCityHighways(

        final OvermapGrid grid,

        final OvermapGenerateOptions options,

        final OvermapConnectionRegistry connectionRegistry,

        final OvermapTerrainRegistry oterRegistry,

        final Random rng,

        final List<String> warnings,

        final List<int[]> cityCenters,

        final List<UrbanSite> urbanSites

    ) {

        if (urbanSites != null && urbanSites.size() >= 2) {

            return HighwayGenerator.connectCities(

                grid,

                urbanSites,

                connectionRegistry,

                options,

                oterRegistry,

                rng,

                warnings

            );

        }

        return HighwayGenerator.connectSites(

            grid,

            cityCenters,

            connectionRegistry,

            options,

            oterRegistry,

            rng,

            warnings

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


