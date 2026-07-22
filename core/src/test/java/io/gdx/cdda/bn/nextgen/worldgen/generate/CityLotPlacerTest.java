package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.worldgen.region.CityContentWeights;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CityLotPlacerTest {

    private OvermapGrid grid;
    private OvermapConnectionRegistry connections;
    private OvermapTerrainRegistry oterRegistry;
    private OvermapGenerateOptions options;
    private CityContentWeights content;

    @BeforeEach
    void setUp() throws Exception {
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final RegionSettingsDefinition urbanRegion = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("urban_heavy").orElseThrow();
        content = urbanRegion.getCityContentWeights();
        grid = new OvermapGrid(32, 32, "test_field");
        options = OvermapGenerateOptions.forSize(32, 32)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(false, true, "local_road", "test_river", "test_river");
        BaseTerrainFiller.fill(grid, options, null, oterRegistry, new Random(1L));
    }

    @Test
    void buildingsOnlyBesideRoads() {
        final UrbanSite site = new UrbanSite(16, 16, 6, CityTier.LARGE);
        final CityStreetGenerator.GrowResult streets = CityStreetGenerator.growCity(
            grid, site, connections, options, oterRegistry, new Random(42L), new ArrayList<>()
        );
        final int placed = CityLotPlacer.placeLots(
            grid, site, streets.getStreetNodes(), content, options, oterRegistry,
            new Random(99L), new ArrayList<>()
        );

        assertTrue(placed >= 5, "expected lots beside streets, got " + placed);
        assertTrue(allBuildingsHaveRoadNeighbor(), "every building must touch a road");
    }

    @Test
    void shopsBiasTowardCenterVsHouses() {
        final UrbanSite site = new UrbanSite(16, 16, 8, CityTier.HUGE);
        final CityStreetGenerator.GrowResult streets = CityStreetGenerator.growCity(
            grid, site, connections, options, oterRegistry, new Random(11L), new ArrayList<>()
        );
        CityLotPlacer.placeLots(
            grid, site, streets.getStreetNodes(), content, options, oterRegistry,
            new Random(123L), new ArrayList<>()
        );

        final List<Double> shopDists = new ArrayList<>();
        final List<Double> houseDists = new ArrayList<>();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                final String base = OvermapTerrainResolver.stripRotation(id);
                if ("test_shop".equals(base)) {
                    shopDists.add(Math.hypot(x - 16, y - 16));
                } else if ("test_urban_house".equals(base)) {
                    houseDists.add(Math.hypot(x - 16, y - 16));
                }
            }
        }
        assertTrue(!shopDists.isEmpty(), "expected at least one shop");
        assertTrue(!houseDists.isEmpty(), "expected at least one house");
        assertTrue(
            mean(shopDists) < mean(houseDists) + 1.5,
            "shops should be closer to center than houses (shops=" + mean(shopDists)
                + ", houses=" + mean(houseDists) + ")"
        );
    }

    @Test
    void buildingsFaceAdjacentStreet() {
        final UrbanSite site = new UrbanSite(16, 16, 6, CityTier.LARGE);
        final CityStreetGenerator.GrowResult streets = CityStreetGenerator.growCity(
            grid, site, connections, options, oterRegistry, new Random(42L), new ArrayList<>()
        );
        CityLotPlacer.placeLots(
            grid, site, streets.getStreetNodes(), content, options, oterRegistry,
            new Random(99L), new ArrayList<>()
        );

        int oriented = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if (!isBuilding(id)) {
                    continue;
                }
                if (id.endsWith("_north") || id.endsWith("_east")
                    || id.endsWith("_south") || id.endsWith("_west")) {
                    oriented++;
                }
            }
        }
        assertTrue(oriented >= 3, "expected street-facing rotated lots, got " + oriented);
    }

    private boolean allBuildingsHaveRoadNeighbor() {
        final int[][] deltas = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if (!isBuilding(id)) {
                    continue;
                }
                boolean roadNeighbor = false;
                for (final int[] d : deltas) {
                    final int nx = x + d[0];
                    final int ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                        continue;
                    }
                    final String n = grid.getOmtId(nx, ny);
                    if (n != null && n.startsWith("test_road")) {
                        roadNeighbor = true;
                        break;
                    }
                }
                if (!roadNeighbor) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isBuilding(final String id) {
        if (id == null) {
            return false;
        }
        final String base = OvermapTerrainResolver.stripRotation(id);
        return "test_urban_house".equals(base)
            || "test_shop".equals(base)
            || "test_park".equals(base)
            || "test_finale".equals(base);
    }

    private static double mean(final List<Double> values) {
        double sum = 0;
        for (final double v : values) {
            sum += v;
        }
        return sum / values.size();
    }
}
