package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HighwayGeneratorCityTest {

    @Test
    void connectsTwoUrbanSitesWithRoadTerrain() {
        final OvermapGrid grid = new OvermapGrid(24, 24, "test_field");
        final OvermapConnectionRegistry connections = new OvermapConnectionRegistry();
        connections.put(new OvermapConnectionDefinition(
            "local_road",
            "test_road",
            "test_bridge",
            Arrays.asList("test_road", "test_road_ns", "test_road_ew")
        ));
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(24, 24)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(false, true, "local_road", "test_river", "test_river");

        final List<UrbanSite> sites = Arrays.asList(
            new UrbanSite(4, 4, 3, CityTier.NORMAL),
            new UrbanSite(18, 16, 3, CityTier.NORMAL)
        );
        final int painted = HighwayGenerator.connectCities(
            grid,
            sites,
            connections,
            options,
            null,
            new Random(11L),
            new ArrayList<>()
        );

        assertTrue(painted >= 5, "expected highway cells between urban centers, got " + painted);
        assertTrue(
            isReachableViaRoads(grid, 4, 4, 18, 16),
            "expected road path between city centers"
        );
    }

    @Test
    void minimumSpanningTreeConnectsThreeUrbanSites() {
        final OvermapGrid grid = new OvermapGrid(32, 32, "test_field");
        final OvermapConnectionRegistry connections = new OvermapConnectionRegistry();
        connections.put(new OvermapConnectionDefinition(
            "local_road",
            "test_road",
            "test_bridge",
            Arrays.asList("test_road", "test_road_ns", "test_road_ew")
        ));
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(32, 32)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(false, true, "local_road", "test_river", "test_river");

        final List<UrbanSite> sites = Arrays.asList(
            new UrbanSite(4, 4, 2, CityTier.NORMAL),
            new UrbanSite(26, 6, 2, CityTier.NORMAL),
            new UrbanSite(14, 24, 2, CityTier.NORMAL)
        );
        final int painted = HighwayGenerator.connectCities(
            grid,
            sites,
            connections,
            options,
            null,
            new Random(13L),
            new ArrayList<>()
        );

        assertTrue(painted >= 8, "expected MST highway cells, got " + painted);
        assertTrue(isReachableViaRoads(grid, 4, 4, 26, 6));
        assertTrue(isReachableViaRoads(grid, 4, 4, 14, 24));
        assertTrue(isReachableViaRoads(grid, 26, 6, 14, 24));
    }

    private static boolean isReachableViaRoads(
        final OvermapGrid grid,
        final int startX,
        final int startY,
        final int goalX,
        final int goalY
    ) {
        final ArrayDeque<int[]> queue = new ArrayDeque<>();
        final Set<Long> visited = new HashSet<>();
        queue.add(new int[] { startX, startY });
        visited.add(pack(startX, startY));

        while (!queue.isEmpty()) {
            final int[] current = queue.removeFirst();
            final int x = current[0];
            final int y = current[1];
            if (x == goalX && y == goalY) {
                return true;
            }
            for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                final int nx = x + step[0];
                final int ny = y + step[1];
                if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                    continue;
                }
                if (!isRoadTerrain(grid.getOmtId(nx, ny))) {
                    continue;
                }
                final long key = pack(nx, ny);
                if (visited.add(key)) {
                    queue.add(new int[] { nx, ny });
                }
            }
        }
        return false;
    }

    private static long pack(final int x, final int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private static boolean isRoadTerrain(final String id) {
        return id != null
            && (id.startsWith("test_road") || "test_bridge".equals(id));
    }
}
