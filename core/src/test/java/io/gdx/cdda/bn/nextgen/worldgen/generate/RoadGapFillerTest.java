package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadGapFillerTest {

    private OvermapGrid grid;
    private OvermapGenerateOptions options;

    @BeforeEach
    void setUp() throws Exception {
        final OvermapTerrainRegistry oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        grid = new OvermapGrid(16, 16, "test_field");
        options = OvermapGenerateOptions.forSize(16, 16)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, true, "local_road", "test_river", "test_river");
        BaseTerrainFiller.fill(grid, options, null, oterRegistry, new Random(1L));
    }

    @Test
    void pavesHoleWithThreeRoadNeighbors() {
        grid.setOmtId(5, 4, "test_road_end_south");
        grid.setOmtId(4, 5, "test_road_end_east");
        grid.setOmtId(6, 5, "test_road_end_west");
        grid.setOmtId(5, 5, "test_field");

        final int filled = RoadGapFiller.fill(grid, "test_road", options);
        assertEquals(1, filled);
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(5, 5)));
    }

    @Test
    void doesNotPaveUShapeThatWouldMakeTwoByTwo() {
        for (int x = 4; x <= 6; x++) {
            grid.setOmtId(x, 4, "test_road_ew");
        }
        grid.setOmtId(4, 5, "test_road_ns");
        grid.setOmtId(5, 5, "test_field");
        grid.setOmtId(6, 5, "test_road_ns");

        final int filled = RoadGapFiller.fill(grid, "test_road", options);
        assertEquals(0, filled);
        assertEquals("test_field", grid.getOmtId(5, 5));
    }

    @Test
    void doesNotPaveLotRowBetweenParallelStreets() {
        for (int x = 2; x <= 8; x++) {
            grid.setOmtId(x, 4, "test_road_ew");
            grid.setOmtId(x, 5, "test_field");
            grid.setOmtId(x, 6, "test_road_ew");
        }
        final int filled = RoadGapFiller.fill(grid, "test_road", options);
        assertEquals(0, filled);
        assertEquals("test_field", grid.getOmtId(5, 5));
    }

    @Test
    void dissolverOnlyClearsSolidTwoByTwo() {
        // Legitimate L-junction must survive.
        grid.setOmtId(5, 5, "test_road_ns");
        grid.setOmtId(5, 6, "test_road_ne");
        grid.setOmtId(6, 6, "test_road_ew");
        ParallelRoadLaneDissolver.dissolve(grid, "test_field");
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(5, 5)));
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(5, 6)));
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(6, 6)));

        // Solid 2x2 must lose the south pair.
        grid.setOmtId(8, 8, "test_road_ew");
        grid.setOmtId(9, 8, "test_road_ew");
        grid.setOmtId(8, 9, "test_road_ew");
        grid.setOmtId(9, 9, "test_road_ew");
        ParallelRoadLaneDissolver.dissolve(grid, "test_field");
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(8, 8)));
        assertTrue(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(9, 8)));
        assertFalse(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(8, 9)));
        assertFalse(UrbanTerrainClearables.isRoadFamily(grid.getOmtId(9, 9)));
    }
}
