package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForestGeneratorTest {

    private OvermapTerrainRegistry oterRegistry;
    private RegionSettingsDefinition forestHeavy;
    private RegionSettingsDefinition forestLight;

    @BeforeEach
    void loadFixtures() throws Exception {
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry regions = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        forestHeavy = regions.find("forest_heavy").orElseThrow();
        forestLight = regions.find("forest_light").orElseThrow();
    }

    @Test
    void onlyDefaultOterCellsBecomeForest() {
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(4242L)
            .withTerrainIds("open_air", "test_field");
        final OvermapGrid grid = new OvermapGrid(16, 16, "open_air");
        BaseTerrainFiller.fill(grid, options, forestHeavy, oterRegistry, null);
        grid.setOmtId(4, 4, "test_river");
        grid.setOmtId(8, 8, "test_lake");
        final String riverBefore = grid.getOmtId(4, 4);
        final String lakeBefore = grid.getOmtId(8, 8);

        final int painted = ForestGenerator.placeForests(
            grid,
            options,
            forestHeavy,
            oterRegistry,
            new ArrayList<>()
        );

        assertTrue(painted > 0);
        assertEquals(riverBefore, grid.getOmtId(4, 4));
        assertEquals(lakeBefore, grid.getOmtId(8, 8));
        assertTrue(countOmt(grid, "test_field") > 0);
    }

    @Test
    void lowerThresholdProducesMoreForestThanHigherThreshold() {
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(32, 32)
            .withSeed(9001L)
            .withTerrainIds("open_air", "test_field");
        final OvermapGrid heavy = new OvermapGrid(32, 32, "open_air");
        final OvermapGrid light = new OvermapGrid(32, 32, "open_air");
        BaseTerrainFiller.fill(heavy, options, forestHeavy, oterRegistry, null);
        BaseTerrainFiller.fill(light, options, forestLight, oterRegistry, null);
        ForestGenerator.placeForests(heavy, options, forestHeavy, oterRegistry, new ArrayList<>());
        ForestGenerator.placeForests(light, options, forestLight, oterRegistry, new ArrayList<>());

        assertTrue(countOmt(heavy, "test_field") > countOmt(light, "test_field"));
    }

    private static int countOmt(final OvermapGrid grid, final String omtId) {
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
