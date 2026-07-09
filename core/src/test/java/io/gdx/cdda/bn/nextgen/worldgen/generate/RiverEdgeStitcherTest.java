package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RiverEdgeStitcherTest {

    @Test
    void copiesRiverFromNorthNeighborEdge() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final RegionSettingsDefinition region = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("default").orElseThrow();
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(1L)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "test_local_road", "test_river", "test_river");

        final OvermapGrid north = new OvermapGrid(16, 16, "test_field");
        for (int x = 4; x <= 8; x++) {
            north.setOmtId(x, 15, "test_river");
        }

        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        final RiverEdgeStitcher.RiverEndpointPlan plan = RiverEdgeStitcher.stitchAndCollect(
            grid,
            new OvermapNeighborContext(north, null, null, null),
            options,
            registry,
            "test_river",
            region.getRiverScale(),
            new Random(1L)
        );

        assertTrue(plan.getStitchedCells() >= 3, "expected stitched river cells on north edge");
        for (int x = 4; x <= 8; x++) {
            assertTrue(
                HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, 0), options, registry),
                "expected river at (" + x + ",0), got " + grid.getOmtId(x, 0)
            );
        }
    }
}
