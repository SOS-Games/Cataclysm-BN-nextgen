package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForestTrailGeneratorTest {

    private OvermapTerrainRegistry oterRegistry;
    private OvermapConnectionRegistry connections;
    private RegionSettingsDefinition trailRegion;
    private RegionSettingsDefinition defaultRegion;

    @BeforeEach
    void setUp() throws Exception {
        final MapgenScanOptions regionOptions = MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot());
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final RegionSettingsRegistry registry = RegionSettingsLoader.load(regionOptions).getRegistry();
        trailRegion = registry.find("forest_trails").orElseThrow();
        defaultRegion = registry.find("default").orElseThrow();
    }

    @Test
    void carvesTrailsThroughContiguousForest() {
        final OvermapGrid grid = new OvermapGrid(24, 24, "open_air");
        fillRect(grid, 3, 3, 18, 18, "test_field");

        final int painted = ForestTrailGenerator.placeAll(
            grid,
            trailRegion,
            connections,
            OvermapGenerateOptions.forSize(24, 24).withTerrainIds("open_air", "test_field"),
            oterRegistry,
            new java.util.Random(42L),
            new ArrayList<>()
        );

        assertTrue(painted >= 3, "expected trail cells, got " + painted);
        assertTrue(countTerrain(grid, "test_forest_trail") >= 3);
    }

    @Test
    void skipsWhenChanceDisabled() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "open_air");
        fillRect(grid, 2, 2, 12, 12, "test_field");
        final RegionSettingsDefinition defaultRegion = this.defaultRegion;

        final int painted = ForestTrailGenerator.placeAll(
            grid,
            defaultRegion,
            connections,
            OvermapGenerateOptions.forSize(16, 16).withTerrainIds("open_air", "test_field"),
            oterRegistry,
            new java.util.Random(1L),
            new ArrayList<>()
        );

        assertEquals(0, painted);
        assertEquals(0, countTerrain(grid, "test_forest_trail"));
    }

    @Test
    void placesTrailheadNearRoadAtTrailEnd() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "open_air");
        fillRect(grid, 2, 2, 12, 12, "test_field");
        for (int y = 4; y <= 10; y++) {
            grid.setOmtId(7, y, "test_forest_trail");
        }
        grid.setOmtId(7, 3, "test_field");
        grid.setOmtId(7, 11, "test_field");
        grid.setOmtId(5, 2, "test_road");

        final OvermapConnectionRegistry localConnections = new OvermapConnectionRegistry();
        localConnections.put(connections.find("test_forest_trail").orElseThrow());
        localConnections.put(new OvermapConnectionDefinition(
            "local_road",
            "test_road",
            "test_bridge",
            Arrays.asList("test_road", "test_road_ns", "test_road_ew")
        ));

        final int painted = ForestTrailGenerator.placeAll(
            grid,
            trailRegion,
            localConnections,
            OvermapGenerateOptions.forSize(16, 16).withTerrainIds("open_air", "test_field"),
            oterRegistry,
            new java.util.Random(7L),
            new ArrayList<>()
        );

        assertTrue(painted >= 1, "expected trailhead placement, got " + painted);
        assertTrue(countTerrain(grid, "test_trailhead") >= 1);
    }

    private static void fillRect(
        final OvermapGrid grid,
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final String omtId
    ) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                grid.setOmtId(x, y, omtId);
            }
        }
    }

    private static int countTerrain(final OvermapGrid grid, final String omtId) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (omtId.equals(grid.getOmtId(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }
}
