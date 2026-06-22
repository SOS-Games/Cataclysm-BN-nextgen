package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CityPlacerRegionTest {

    private CityBuildingRegistry buildings;
    private io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition forestHeavy;

    @BeforeEach
    void loadFixtures() throws Exception {
        final MapgenPreviewService mapgen = new MapgenPreviewService();
        mapgen.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        buildings = mapgen.getCityBuildings();
        forestHeavy = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("forest_heavy").orElseThrow();
    }

    @Test
    void weightedRegionFavorsMultitileBuilding() throws Exception {
        final OvermapGrid grid = new OvermapGrid(16, 16, "open_air");
        BaseTerrainFiller.fill(
            grid,
            OvermapGenerateOptions.forSize(16, 16).withTerrainIds("open_air", "test_field"),
            forestHeavy,
            OvermapTerrainLoader.load(
                OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
            ).getRegistry(),
            null
        );

        int multitileCells = 0;
        int duplexCells = 0;
        for (int attempt = 0; attempt < 30; attempt++) {
            final OvermapGrid attemptGrid = copyGrid(grid);
            CityPlacer.placeAll(
                attemptGrid,
                buildings,
                null,
                OvermapGenerateOptions.forSize(16, 16).withQuotas(3, 0),
                forestHeavy,
                new Random(1000L + attempt),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
            );
            multitileCells += countOmtPrefix(attemptGrid, "test_multitile");
            duplexCells += countOmtPrefix(attemptGrid, "test_duplex");
        }

        assertTrue(multitileCells > duplexCells * 3,
            "expected weighted region to favor test_multitile, got multitile=" + multitileCells
                + " duplex=" + duplexCells);
    }

    private static OvermapGrid copyGrid(final OvermapGrid source) {
        final OvermapGrid copy = new OvermapGrid(source.width(), source.height(), "open_air");
        for (int y = 0; y < source.height(); y++) {
            for (int x = 0; x < source.width(); x++) {
                copy.setOmtId(x, y, source.getOmtId(x, y));
            }
        }
        return copy;
    }

    private static int countOmtPrefix(final OvermapGrid grid, final String prefix) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (grid.getOmtId(x, y).startsWith(prefix)) {
                    count++;
                }
            }
        }
        return count;
    }
}
