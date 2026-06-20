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
        connections.put(new OvermapConnectionDefinition("test_local_road", "test_road", Arrays.asList("test_road")));
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
            new ArrayList<>()
        );
        assertTrue(painted >= 3, "expected road cells between sites, got " + painted);

        boolean foundRoad = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if ("test_road".equals(grid.getOmtId(x, y))) {
                    foundRoad = true;
                }
            }
        }
        assertTrue(foundRoad);
    }
}
