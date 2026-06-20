package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

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
        if (options == null) {
            throw new IllegalArgumentException("options is required");
        }
        final List<String> warnings = new ArrayList<>();
        final Random rng = new Random(options.getSeed() ^ 0x504C4143L);
        final OvermapGrid grid = new OvermapGrid(
            options.getWidth(),
            options.getHeight(),
            options.getFieldId()
        );

        BaseTerrainFiller.fill(grid, options, oterRegistry, rng);

        final int riversCarved = RiverGenerator.carve(grid, options, oterRegistry, rng, warnings);

        final CityBuildingRegistry registry = buildings == null ? CityBuildingRegistry.empty() : buildings;
        final List<int[]> placedSites = new ArrayList<>();
        final int cities = CityPlacer.placeAll(
            grid,
            registry,
            oterRegistry,
            options,
            rng,
            warnings,
            placedSites
        );
        final int specials = StaticSpecialPlacer.placeAll(
            grid,
            registry,
            oterRegistry,
            options,
            rng,
            warnings,
            placedSites
        );
        final int mutablePlaced = MutableSpecialPlacer.placeAll(
            grid,
            mutableSpecials,
            oterRegistry,
            options,
            rng,
            warnings,
            placedSites
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

        return new OvermapGenerateResult(
            grid,
            warnings,
            cities,
            specials,
            mutablePlaced,
            riversCarved,
            roadCells
        );
    }
}
