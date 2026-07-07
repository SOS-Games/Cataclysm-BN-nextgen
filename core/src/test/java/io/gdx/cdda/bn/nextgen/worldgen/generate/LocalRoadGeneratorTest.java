package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalRoadGeneratorTest {

    private OvermapGrid grid;
    private UrbanSite site;
    private OvermapConnectionRegistry connections;
    private io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry oterRegistry;
    private OvermapGenerateOptions options;

    @BeforeEach
    void setUp() throws Exception {
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        grid = new OvermapGrid(24, 24, "test_field");
        BaseTerrainFiller.fill(grid, OvermapGenerateOptions.forSize(24, 24), null, oterRegistry, new Random(1L));
        connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final RegionSettingsDefinition urbanRegion = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("urban_heavy").orElseThrow();
        site = new UrbanSite(12, 12, 5, CityTier.LARGE);
        UrbanOmtPlacer.fillBlob(
            grid,
            site,
            urbanRegion.getCityContentWeights(),
            OvermapGenerateOptions.forSize(24, 24),
            oterRegistry,
            new Random(42L),
            new ArrayList<>()
        );
        options = OvermapGenerateOptions.forSize(24, 24)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(false, true, "local_road", "test_river", "test_river");
    }

    @Test
    void carvesRoadGridInsideUrbanBlob() {
        final int painted = LocalRoadGenerator.carveSites(
            grid,
            List.of(site),
            connections,
            options,
            oterRegistry,
            new Random(7L),
            new ArrayList<>()
        );

        assertTrue(painted >= 5, "expected several local road cells, got " + painted);
        assertTrue(countRoadCellsInsideSite() >= 5);
    }

    @Test
    void centerReachableFromBlobEdgeViaRoads() {
        LocalRoadGenerator.carveSites(
            grid,
            List.of(site),
            connections,
            options,
            oterRegistry,
            new Random(7L),
            new ArrayList<>()
        );

        final int[] edgeRoad = findEdgeRoadCell();
        assertTrue(edgeRoad != null, "expected a road cell on the blob edge");
        assertTrue(
            isReachableViaRoads(edgeRoad[0], edgeRoad[1], site.getCenterX(), site.getCenterY()),
            "city center should be reachable from edge roads"
        );
    }

    private int countRoadCellsInsideSite() {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (site.contains(x, y) && isRoadTerrain(grid.getOmtId(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }

    private int[] findEdgeRoadCell() {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!site.contains(x, y) || !isRoadTerrain(grid.getOmtId(x, y))) {
                    continue;
                }
                if (x == site.getCenterX() - site.getRadius()
                    || x == site.getCenterX() + site.getRadius()
                    || y == site.getCenterY() - site.getRadius()
                    || y == site.getCenterY() + site.getRadius()) {
                    return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private boolean isReachableViaRoads(final int startX, final int startY, final int goalX, final int goalY) {
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
                if (!site.contains(nx, ny) || !isRoadTerrain(grid.getOmtId(nx, ny))) {
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
