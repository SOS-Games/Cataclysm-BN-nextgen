package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HighwayGeneratorTest {

    @Test
    void connectsTwoSitesWithRoadTerrain() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        final OvermapConnectionRegistry connections = new OvermapConnectionRegistry();
        connections.put(new OvermapConnectionDefinition(
            "test_local_road",
            "test_road",
            "test_bridge",
            Arrays.asList("test_road", "test_road_ns", "test_road_ew")
        ));
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(false, true, "test_local_road", "test_river", "test_river");

        final int painted = HighwayGenerator.connectSites(
            grid,
            Arrays.asList(new int[] { 2, 2 }, new int[] { 12, 10 }),
            connections,
            options,
            null,
            new Random(3L),
            new ArrayList<String>()
        );
        assertTrue(painted >= 3, "expected road cells between sites, got " + painted);

        boolean foundRoad = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if ("test_road".equals(id) || "test_road_ns".equals(id) || "test_road_ew".equals(id)) {
                    foundRoad = true;
                }
            }
        }
        assertTrue(foundRoad);
    }

    @Test
    void paintsBridgeOverRiverCell() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        grid.setOmtId(8, 8, "test_river");
        final OvermapConnectionRegistry connections = new OvermapConnectionRegistry();
        connections.put(new OvermapConnectionDefinition(
            "test_directional_road",
            "test_road",
            "test_bridge",
            Arrays.asList("test_road_ns", "test_road_ew")
        ));
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(false, true, "test_directional_road", "test_river", "test_river");

        final int painted = HighwayGenerator.connectSites(
            grid,
            Arrays.asList(new int[] { 8, 2 }, new int[] { 8, 14 }),
            connections,
            options,
            null,
            new Random(5L),
            new ArrayList<String>()
        );
        assertTrue(painted >= 1);
        assertTrue("test_bridge".equals(grid.getOmtId(8, 8)) || "test_road_ns".equals(grid.getOmtId(8, 8)));
    }
}
