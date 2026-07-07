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
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndergroundNetworkGeneratorTest {

    private OvermapTerrainRegistry oterRegistry;
    private OvermapConnectionRegistry connections;
    private RegionSettingsDefinition undergroundRegion;
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
        undergroundRegion = registry.find("underground_networks").orElseThrow();
        defaultRegion = registry.find("default").orElseThrow();
    }

    @Test
    void carvesSubwayRailAndSewerNetworks() {
        final OvermapGrid grid = new OvermapGrid(32, 32, "test_field");
        final List<UrbanSite> sites = Arrays.asList(
            new UrbanSite(8, 8, 4, CityTier.NORMAL),
            new UrbanSite(24, 20, 4, CityTier.NORMAL)
        );
        fillUrbanBlobs(grid, sites);

        final int painted = UndergroundNetworkGenerator.placeAll(
            grid,
            sites,
            undergroundRegion,
            connections,
            OvermapGenerateOptions.forSize(32, 32).withTerrainIds("test_field", "test_field"),
            oterRegistry,
            new Random(17L),
            new ArrayList<>()
        );

        assertTrue(painted >= 6, "expected underground cells, got " + painted);
        assertTrue(countTerrain(grid, "test_subway") >= 2);
        assertTrue(countTerrain(grid, "test_railroad") >= 2);
        assertTrue(countTerrain(grid, "test_sewer") >= 2);
    }

    @Test
    void skipsWhenRegionDisablesUndergroundNetworks() {
        final OvermapGrid grid = new OvermapGrid(24, 24, "test_field");
        final RegionSettingsDefinition defaultRegion = this.defaultRegion;
        final List<UrbanSite> sites = Arrays.asList(
            new UrbanSite(6, 6, 3, CityTier.NORMAL),
            new UrbanSite(18, 16, 3, CityTier.NORMAL)
        );

        final int painted = UndergroundNetworkGenerator.placeAll(
            grid,
            sites,
            defaultRegion,
            connections,
            OvermapGenerateOptions.forSize(24, 24).withTerrainIds("test_field", "test_field"),
            oterRegistry,
            new Random(3L),
            new ArrayList<>()
        );

        assertEquals(0, painted);
    }

    @Test
    void subwayGeneratorConnectsTwoUrbanSites() {
        final OvermapGrid grid = new OvermapGrid(24, 24, "test_field");
        final List<UrbanSite> sites = Arrays.asList(
            new UrbanSite(4, 4, 3, CityTier.NORMAL),
            new UrbanSite(18, 16, 3, CityTier.NORMAL)
        );

        final int painted = SubwayGenerator.connectCities(
            grid,
            sites,
            connections,
            OvermapGenerateOptions.forSize(24, 24).withTerrainIds("test_field", "test_field"),
            oterRegistry,
            new Random(5L),
            new ArrayList<>()
        );

        assertTrue(painted >= 3, "expected subway cells, got " + painted);
        assertTrue(countTerrain(grid, "test_subway") >= 3);
    }

    private static void fillUrbanBlobs(final OvermapGrid grid, final List<UrbanSite> sites) {
        for (final UrbanSite site : sites) {
            for (int y = site.getCenterY() - site.getRadius(); y <= site.getCenterY() + site.getRadius(); y++) {
                for (int x = site.getCenterX() - site.getRadius(); x <= site.getCenterX() + site.getRadius(); x++) {
                    grid.setOmtId(x, y, "test_urban_house");
                }
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
