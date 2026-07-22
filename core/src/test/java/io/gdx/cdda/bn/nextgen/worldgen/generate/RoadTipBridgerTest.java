package io.gdx.cdda.bn.nextgen.worldgen.generate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

class RoadTipBridgerTest {

    @Test
    void bridgesOneCellGapBetweenRoadTips() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        grid.setOmtId(5, 3, "test_road");
        grid.setOmtId(5, 4, "test_road"); // tip
        grid.setOmtId(5, 5, "test_field"); // gap
        grid.setOmtId(5, 6, "test_road"); // tip
        grid.setOmtId(5, 7, "test_road");

        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withTerrainIds("test_field", "test_field");
        final int paved = RoadTipBridger.bridge(grid, "test_road", options);
        assertTrue(paved >= 1);
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(5, 5)));
    }

    @Test
    void bridgesTipAcrossForestToHighwayColumn() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        grid.setOmtId(4, 7, "test_road");
        grid.setOmtId(5, 7, "test_road"); // tip facing east
        grid.setOmtId(6, 7, "forest_thick"); // gap
        grid.setOmtId(7, 7, "test_road"); // highway column
        grid.setOmtId(7, 6, "test_road");
        grid.setOmtId(7, 8, "test_road");

        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withTerrainIds("test_field", "test_field");
        final int paved = RoadTipBridger.bridge(grid, "test_road", options);
        assertTrue(paved >= 1, "expected bridge across forest gap, paved=" + paved);
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(6, 7)));
    }

    @Test
    void bridgesDiagonalHighwayCityNearMiss() {
        // City tip at (5,6) facing north; highway tip at (6,5) — L-join via (5,5) or (6,6).
        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        grid.setOmtId(5, 7, "test_road");
        grid.setOmtId(5, 6, "test_road"); // city tip
        grid.setOmtId(5, 5, "forest_thick");
        grid.setOmtId(6, 5, "test_road"); // highway
        grid.setOmtId(6, 4, "test_road");

        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withTerrainIds("test_field", "test_field");
        final int paved = RoadTipBridger.bridge(grid, "test_road", options);
        assertTrue(paved >= 1, "expected diagonal corner pave, paved=" + paved);
        final boolean joinedViaNorthWest = UrbanTerrainClearables.isRoadFamily(grid.getOmtId(5, 5));
        final boolean joinedViaSouthEast = UrbanTerrainClearables.isRoadFamily(grid.getOmtId(6, 6));
        assertTrue(joinedViaNorthWest || joinedViaSouthEast, "expected L-join at (5,5) or (6,6)");
        // City stub and highway must share a connected road component.
        assertTrue(
            UrbanTerrainClearables.isRoadFamily(grid.getOmtId(5, 6))
                && (joinedViaNorthWest
                    ? UrbanTerrainClearables.isRoadFamily(grid.getOmtId(6, 5))
                    : UrbanTerrainClearables.isRoadFamily(grid.getOmtId(6, 6))),
            "city and highway should touch via the paved corner"
        );
    }

    @Test
    void doesNotBridgeLotRowBetweenParallelStreets() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        for (int x = 2; x <= 8; x++) {
            grid.setOmtId(x, 4, "test_road");
            grid.setOmtId(x, 5, "test_field");
            grid.setOmtId(x, 6, "test_road");
        }
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withTerrainIds("test_field", "test_field");
        final int paved = RoadTipBridger.bridge(grid, "test_road", options);
        assertEquals(0, paved);
        assertEquals("test_field", grid.getOmtId(5, 5));
    }

    @Test
    void highwayJoinsCityStreetAcrossFormerParallelBlock() {
        // City EW street, highway approaching from north on same column — must connect.
        final OvermapGrid grid = new OvermapGrid(24, 24, "test_field");
        for (int x = 4; x <= 14; x++) {
            grid.setOmtId(x, 12, "test_road");
        }
        for (int x = 8; x <= 12; x++) {
            grid.setOmtId(x, 11, "test_road");
            grid.setOmtId(x, 13, "test_road");
        }
        final OvermapConnectionRegistry connections = new OvermapConnectionRegistry();
        connections.put(new OvermapConnectionDefinition(
            "test_local_road",
            "test_road",
            "test_bridge",
            Arrays.asList("test_road", "test_road_ns", "test_road_ew")
        ));
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(24, 24)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(false, true, "test_local_road", "test_river", "test_river");

        HighwayGenerator.connectSites(
            grid,
            Arrays.asList(new int[] { 10, 2 }, new int[] { 10, 12 }),
            connections,
            options,
            null,
            new Random(1L),
            new ArrayList<>()
        );
        RoadTipBridger.bridge(grid, "test_road", options);

        boolean connected = true;
        for (int y = 2; y <= 12; y++) {
            if (!UrbanTerrainClearables.isRoadFamily(grid.getOmtId(10, y))) {
                connected = false;
                break;
            }
        }
        assertTrue(connected, "highway column 10 should reach city street at y=12");
    }
}
