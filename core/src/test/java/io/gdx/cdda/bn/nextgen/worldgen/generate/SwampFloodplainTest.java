package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.CityContentWeights;
import io.gdx.cdda.bn.nextgen.worldgen.region.CitySizeSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapForestSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapLakeSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapSpecialSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapTerrainSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwampFloodplainTest {

    @Test
    void riverFloodplainPaintsSwampInForestBufferNotFarForest() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(32, 32)
            .withSeed(4242L)
            .withTerrainIds("open_air", "test_forest_thick");

        final OvermapForestSettings forest = new OvermapForestSettings(
            0.35,
            0.0,
            "test_forest_thick",
            "test_forest_thick",
            8,
            8
        );
        final OvermapTerrainSettings terrain = new OvermapTerrainSettings(
            true,
            0.01,
            0.99,
            "test_swamp",
            "test_beach"
        );
        final RegionSettingsDefinition region = new RegionSettingsDefinition(
            "floodplain_test",
            "open_air",
            forest,
            OvermapLakeSettings.disabled(),
            CityContentWeights.empty(),
            CitySizeSettings.disabled(),
            OvermapSpecialSettings.disabled(),
            terrain
        );

        final OvermapGrid grid = new OvermapGrid(32, 32, "open_air");
        for (int x = 0; x < grid.width(); x++) {
            grid.setOmtId(x, 16, "test_river");
        }
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (y >= 6 && y <= 14 && y != 16) {
                    grid.setOmtId(x, y, "test_forest_thick");
                }
                if (y <= 2) {
                    grid.setOmtId(x, y, "test_forest_thick");
                }
            }
        }

        final int painted = SwampGenerator.fill(
            grid,
            options,
            region,
            registry,
            new Random(4242L),
            new ArrayList<>()
        );

        assertTrue(painted > 0, "expected swamp in river floodplain buffer");
        assertTrue(countSwampInRow(grid, 10) > 0, "forest within buffer should swamp");
        assertEquals(0, countSwampInRow(grid, 1), "forest outside buffer should not floodplain-swamp");
    }

    @Test
    void nonForestCellsAdjacentToRiverStayUnchanged() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(99L)
            .withTerrainIds("test_field", "test_forest_thick");

        final OvermapForestSettings forest = new OvermapForestSettings(
            0.35,
            0.0,
            "test_forest_thick",
            "test_forest_thick",
            5,
            5
        );
        final OvermapTerrainSettings terrain = new OvermapTerrainSettings(
            true,
            0.01,
            0.99,
            "test_swamp",
            "test_beach"
        );
        final RegionSettingsDefinition region = new RegionSettingsDefinition(
            "floodplain_field_test",
            "open_air",
            forest,
            OvermapLakeSettings.disabled(),
            CityContentWeights.empty(),
            CitySizeSettings.disabled(),
            OvermapSpecialSettings.disabled(),
            terrain
        );

        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        for (int x = 0; x < grid.width(); x++) {
            grid.setOmtId(x, 8, "test_river");
        }

        SwampGenerator.fill(grid, options, region, registry, new Random(99L), new ArrayList<>());

        for (int x = 0; x < grid.width(); x++) {
            if (x == 8) {
                continue;
            }
            assertEquals("test_field", grid.getOmtId(x, 7), "non-forest cells must not become swamp");
            assertEquals("test_field", grid.getOmtId(x, 9), "non-forest cells must not become swamp");
        }
    }

    private static int countSwampInRow(final OvermapGrid grid, final int y) {
        int count = 0;
        for (int x = 0; x < grid.width(); x++) {
            if ("test_swamp".equals(grid.getOmtId(x, y))) {
                count++;
            }
        }
        return count;
    }
}
