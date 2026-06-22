package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionSpecialPlacerTest {

    private CityBuildingRegistry buildings;
    private RegionSettingsDefinition specialHeavy;
    private RegionSettingsDefinition defaultRegion;

    @BeforeEach
    void loadFixtures() throws Exception {
        final MapgenPreviewService mapgen = new MapgenPreviewService();
        mapgen.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        buildings = mapgen.getCityBuildings();
        final io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry regions = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        specialHeavy = regions.find("special_heavy").orElseThrow();
        defaultRegion = regions.find("default").orElseThrow();
    }

    @Test
    void heavyRegionPlacesMoreFarmSpecialCellsThanDefault() throws Exception {
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(32, 32)
            .withTerrainIds("open_air", "test_field");
        final io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry oterRegistry =
            OvermapTerrainLoader.load(
                OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
            ).getRegistry();

        final OvermapGrid heavyGrid = new OvermapGrid(32, 32, "open_air");
        BaseTerrainFiller.fill(heavyGrid, options, specialHeavy, oterRegistry, null);
        RegionSpecialPlacer.placeAll(
            heavyGrid,
            buildings,
            oterRegistry,
            options,
            specialHeavy,
            new Random(77L),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );

        final OvermapGrid defaultGrid = new OvermapGrid(32, 32, "open_air");
        BaseTerrainFiller.fill(defaultGrid, options, defaultRegion, oterRegistry, null);
        RegionSpecialPlacer.placeAll(
            defaultGrid,
            buildings,
            oterRegistry,
            options,
            defaultRegion,
            new Random(77L),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );

        assertTrue(countFarmCells(heavyGrid) > countFarmCells(defaultGrid));
    }

    private static int countFarmCells(final OvermapGrid grid) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (grid.getOmtId(x, y).startsWith("test_farm")) {
                    count++;
                }
            }
        }
        return count;
    }
}
