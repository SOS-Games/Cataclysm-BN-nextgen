package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OmLines;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadLinearPolishTest {

    private OvermapTerrainRegistry registry;
    private OvermapConnectionRegistry connections;

    @BeforeEach
    void loadFixtures() throws Exception {
        registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
    }

    @Test
    void omLinesFourWayAndCornerSuffixes() {
        assertEquals("_nesw", OmLines.suffix(OmLines.BITS));
        assertEquals("_ne", OmLines.suffix(OmLines.fromCardinals(true, true, false, false)));
        assertEquals("_ns", OmLines.suffix(OmLines.fromCardinals(true, false, true, false)));
        assertEquals("road_tee", OmLines.mapgenIdFor("road", OmLines.fromCardinals(true, true, true, false)));
    }

    @Test
    void polishesCrossingToNesw() {
        final OvermapGrid grid = new OvermapGrid(5, 5, "test_field");
        grid.setOmtId(2, 1, "test_road");
        grid.setOmtId(2, 2, "test_road");
        grid.setOmtId(2, 3, "test_road");
        grid.setOmtId(1, 2, "test_road");
        grid.setOmtId(3, 2, "test_road");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(5, 5)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, true, "test_local_road", "test_river", "test_river");

        final int polished = RoadConnectionPolisher.polish(grid, options, connections, registry, new ArrayList<>());

        assertTrue(polished >= 1);
        assertEquals("test_road_nesw", grid.getOmtId(2, 2));
        assertEquals("test_road_end_north", grid.getOmtId(2, 1));
        assertEquals("test_road_end_west", grid.getOmtId(1, 2));
        assertEquals("test_road_end_south", grid.getOmtId(2, 3));
        assertEquals("test_road_end_east", grid.getOmtId(3, 2));
    }

    @Test
    void polishesCornerToEs() {
        final OvermapGrid grid = new OvermapGrid(4, 4, "test_field");
        grid.setOmtId(1, 1, "test_road");
        grid.setOmtId(1, 2, "test_road");
        grid.setOmtId(2, 1, "test_road");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(4, 4)
            .withConnectivity(true, true, "test_local_road", "test_river", "test_river");

        RoadConnectionPolisher.polish(grid, options, connections, registry, new ArrayList<>());

        assertEquals("test_road_es", grid.getOmtId(1, 1));
        assertEquals("test_road_end_south", grid.getOmtId(1, 2));
        assertEquals("test_road_end_east", grid.getOmtId(2, 1));
    }

    @Test
    void polishesDeadEnd() {
        final OvermapGrid grid = new OvermapGrid(3, 4, "test_field");
        grid.setOmtId(1, 0, "test_road");
        grid.setOmtId(1, 1, "test_road");
        grid.setOmtId(1, 2, "test_road");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(3, 4)
            .withConnectivity(true, true, "test_local_road", "test_river", "test_river");

        RoadConnectionPolisher.polish(grid, options, connections, registry, new ArrayList<>());

        assertEquals("test_road_end_north", grid.getOmtId(1, 0));
        assertEquals("test_road_ns", grid.getOmtId(1, 1));
        assertEquals("test_road_end_south", grid.getOmtId(1, 2));
    }
}
