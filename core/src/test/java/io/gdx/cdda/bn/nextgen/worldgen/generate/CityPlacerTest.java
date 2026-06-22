package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CityPlacerTest {

    private CityBuildingRegistry buildings;
    private OvermapGenerateOptions options;

    @BeforeEach
    void loadFixtures() throws Exception {
        final MapgenPreviewService mapgen = new MapgenPreviewService();
        mapgen.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        buildings = mapgen.getCityBuildings();
        options = OvermapGenerateOptions.forSize(8, 8).withSeed(42L);
    }

    @Test
    void tryPlaceWritesDuplexOmtOnClearGrid() throws Exception {
        final OvermapGrid grid = new OvermapGrid(8, 8, "open_air");
        BaseTerrainFiller.fill(
            grid,
            options,
            OvermapTerrainLoader.load(
                OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
            ).getRegistry(),
            null
        );
        final CityBuildingDefinition duplex = buildings.findById("test_duplex").orElseThrow();
        final boolean placed = CityPlacer.tryPlace(
            duplex,
            grid,
            null,
            OmtBuildingBlitter.defaultClearableIds(options, null),
            new Random(1L),
            new ArrayList<>(),
            new ArrayList<>()
        );
        assertTrue(placed);

        boolean foundDuplex = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if ("test_duplex_ground_north".equals(grid.getOmtId(x, y))) {
                    foundDuplex = true;
                }
            }
        }
        assertTrue(foundDuplex);
    }

    @Test
    void placeAllRespectsQuota() throws Exception {
        final OvermapGrid grid = new OvermapGrid(16, 16, "field");
        BaseTerrainFiller.fill(grid, OvermapGenerateOptions.forSize(16, 16), null, new Random(7L));
        final int placed = CityPlacer.placeAll(
            grid,
            buildings,
            null,
            OvermapGenerateOptions.forSize(16, 16).withQuotas(2, 0),
            new Random(99L),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
        assertEquals(2, placed);
    }
}
