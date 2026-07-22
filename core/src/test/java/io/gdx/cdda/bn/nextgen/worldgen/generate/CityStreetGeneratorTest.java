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

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CityStreetGeneratorTest {

    private OvermapGrid grid;
    private OvermapConnectionRegistry connections;
    private OvermapTerrainRegistry oterRegistry;
    private OvermapGenerateOptions options;

    @BeforeEach
    void setUp() throws Exception {
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        grid = new OvermapGrid(32, 32, "test_field");
        options = OvermapGenerateOptions.forSize(32, 32)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, true, "local_road", "test_river", "test_river");
        BaseTerrainFiller.fill(grid, options, null, oterRegistry, new Random(1L));
    }

    @Test
    void growsCrossFromCenterWithManhole() {
        final UrbanSite site = new UrbanSite(16, 16, 6, CityTier.LARGE);
        final CityStreetGenerator.GrowResult result = CityStreetGenerator.growCity(
            grid, site, connections, options, oterRegistry, new Random(42L), new ArrayList<>()
        );

        assertTrue(result.getRoadCells() >= 8, "expected street tree, got " + result.getRoadCells());
        final String center = grid.getOmtId(16, 16);
        assertTrue(
            center != null && center.contains("manhole"),
            "center should be manhole, got " + center
        );
        assertTrue(countCardinalRoadArms(16, 16) >= 2, "expected multiple arms from center");
    }

    @Test
    void stopsStreetAtRiver() {
        for (int x = 0; x < 32; x++) {
            grid.setOmtId(x, 20, "test_river");
        }
        final UrbanSite site = new UrbanSite(16, 16, 8, CityTier.HUGE);
        CityStreetGenerator.growCity(
            grid, site, connections, options, oterRegistry, new Random(7L), new ArrayList<>()
        );

        assertTrue(
            "test_river".equals(grid.getOmtId(16, 20)),
            "river cell must not be overwritten by street"
        );
        boolean roadNorthOfRiver = false;
        for (int y = 0; y < 20; y++) {
            final String id = grid.getOmtId(16, y);
            if (id != null && id.startsWith("test_road")) {
                roadNorthOfRiver = true;
                break;
            }
        }
        assertTrue(roadNorthOfRiver, "expected roads north of the river barrier");
    }

    @Test
    void streetsPaveThroughForestAndSwamp() {
        for (int y = 10; y <= 22; y++) {
            grid.setOmtId(16, y, "test_forest_thick");
        }
        for (int x = 10; x <= 22; x++) {
            grid.setOmtId(x, 16, "test_swamp");
        }
        grid.setOmtId(16, 16, "test_field");
        final UrbanSite site = new UrbanSite(16, 16, 6, CityTier.LARGE);
        final CityStreetGenerator.GrowResult result = CityStreetGenerator.growCity(
            grid, site, connections, options, oterRegistry, new Random(42L), new ArrayList<>()
        );

        assertTrue(result.getRoadCells() >= 8, "expected streets through wilderness, got " + result.getRoadCells());
        int pavedForestOrSwamp = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if (id != null && id.startsWith("test_road")
                    && (Math.abs(x - 16) <= 6 || Math.abs(y - 16) <= 6)) {
                    pavedForestOrSwamp++;
                }
            }
        }
        assertTrue(pavedForestOrSwamp >= 8, "roads should replace forest/swamp cells");
    }

    private int countCardinalRoadArms(final int x, final int y) {
        int arms = 0;
        final int[][] deltas = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        for (final int[] d : deltas) {
            final String id = grid.getOmtId(x + d[0], y + d[1]);
            if (id != null && id.startsWith("test_road")) {
                arms++;
            }
        }
        return arms;
    }
}
