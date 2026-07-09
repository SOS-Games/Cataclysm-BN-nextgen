package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapNeighborGridTest {

    @Test
    void westTileStitchesRiverFromEastNeighborEdge() throws Exception {
        final RegionSettingsRegistry regionSettings = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGenerateOptions base = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(77L)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "test_local_road", "test_river", "test_river");

        final OvermapGrid west = new OvermapGrid(16, 16, "test_field");
        for (int y = 2; y <= 6; y++) {
            west.setOmtId(15, y, "test_river");
        }

        final OvermapGrid center = new OvermapGrid(16, 16, "test_field");
        final int stitched = RiverEdgeStitcher.stitchAndCollect(
            center,
            new OvermapNeighborContext(null, null, null, west),
            base,
            registry,
            "test_river",
            regionSettings.find("default").orElseThrow().getRiverScale(),
            new java.util.Random(77L)
        ).getStitchedCells();

        assertTrue(stitched >= 3);
        for (int y = 2; y <= 6; y++) {
            assertTrue(
                HydrologyTerrainClassifier.isRiverOmt(center.getOmtId(0, y), base, registry),
                "expected river at west edge (0," + y + ")"
            );
        }
    }

    @Test
    void batchGenerationSharesRiverAcrossWestEastBoundary() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final RegionSettingsRegistry regionSettings = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGenerateOptions base = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(9001L)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "test_local_road", "test_river", "test_river");

        final OvermapNeighborGrid grid = new OvermapNeighborGrid(
            base,
            io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry.empty(),
            registry,
            new OvermapConnectionRegistry(),
            io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry.empty(),
            regionSettings
        );

        grid.generateBatch(0, 0, 2, 1);
        final OvermapGrid west = grid.getGrid(0, 0);
        final OvermapGrid east = grid.getGrid(1, 0);
        assertTrue(hasRiverOnEastEdge(west, base, registry) || hasRiverOnWestEdge(east, base, registry),
            "expected river cells on shared vertical boundary");
    }

    @Test
    void polishReadsWestNeighborAtEdge() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(8, 8)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "test_local_road", "test_river", "test_river");

        final OvermapGrid west = new OvermapGrid(8, 8, "test_field");
        west.setOmtId(7, 4, "test_river");

        final OvermapGrid center = new OvermapGrid(8, 8, "test_field");
        center.setOmtId(0, 4, "test_river");
        center.setOmtId(1, 4, "test_river");

        final int polished = RiverPolisher.polishDirectional(
            center,
            options,
            registry,
            new ArrayList<>(),
            new OvermapNeighborContext(null, null, null, west)
        );

        assertTrue(polished >= 1);
        assertEquals("test_river_north", center.getOmtId(0, 4));
    }

    private static boolean hasRiverOnEastEdge(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final int x = grid.width() - 1;
        for (int y = 0; y < grid.height(); y++) {
            if (HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, y), options, registry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRiverOnWestEdge(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        for (int y = 0; y < grid.height(); y++) {
            if (HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(0, y), options, registry)) {
                return true;
            }
        }
        return false;
    }
}
