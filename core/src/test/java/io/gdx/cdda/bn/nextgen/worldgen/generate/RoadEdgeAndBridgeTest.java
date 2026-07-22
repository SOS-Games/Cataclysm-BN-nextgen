package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadEdgeAndBridgeTest {

    @Test
    void stitchesRoadFromWestNeighbor() throws Exception {
        final OvermapConnectionRegistry connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGrid west = new OvermapGrid(8, 8, "test_field");
        west.setOmtId(7, 3, "test_road_ew");
        west.setOmtId(7, 4, "test_road_ew");
        final OvermapGrid grid = new OvermapGrid(8, 8, "test_field");

        final int painted = RoadEdgeStitcher.stitch(
            grid,
            new OvermapNeighborContext(null, null, null, west),
            connections
        );

        assertTrue(painted >= 2);
        assertEquals("test_road_ew", grid.getOmtId(0, 3));
        assertEquals("test_road_ew", grid.getOmtId(0, 4));
    }

    @Test
    void elevatesRoadCrossingRiver() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapConnectionRegistry connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGrid grid = new OvermapGrid(5, 5, "test_field");
        grid.setOmtId(2, 1, "test_river");
        grid.setOmtId(2, 2, "test_road_ns");
        grid.setOmtId(2, 3, "test_river");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(5, 5)
            .withConnectivity(true, true, "test_local_road", "test_river", "test_river");

        final int elevated = BridgeElevator.elevateCrossings(grid, options, connections, registry);

        assertTrue(elevated >= 1);
        assertEquals("test_bridge", grid.getOmtId(2, 2));
    }
}
