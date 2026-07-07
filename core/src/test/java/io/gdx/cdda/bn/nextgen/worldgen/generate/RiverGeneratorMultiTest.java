package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiverGeneratorMultiTest {

    private RegionSettingsDefinition lakeRegion;
    private OvermapGenerateOptions options;

    @BeforeEach
    void setUp() throws Exception {
        lakeRegion = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("lake_test").orElseThrow();
        options = OvermapGenerateOptions.forSize(32, 32)
            .withSeed(99L)
            .withTerrainIds("test_field", "test_field")
            .withRegionId("lake_test")
            .withLakesEnabled(true)
            .withConnectivity(true, false, "local_road", "test_river", "test_river");
    }

    @Test
    void secondPassIncreasesRiverCoverageOnLakeRegion() {
        final OvermapGrid onePass = buildLakeGrid();
        final OvermapGrid twoPass = buildLakeGrid();

        RiverGenerator.carve(onePass, options, null, new Random(99L), new ArrayList<>());
        RiverGenerator.carve(twoPass, options, null, new Random(99L), new ArrayList<>());
        RiverGenerator.carve(
            twoPass,
            options,
            null,
            new Random(99L ^ RiverGenerator.SECOND_PASS_SEED_XOR),
            new ArrayList<>()
        );

        assertTrue(
            countRiverCells(twoPass) >= countRiverCells(onePass),
            "second pass should not reduce river coverage"
        );
        assertTrue(
            countRiverCells(twoPass) > countRiverCells(onePass),
            "expected additional river cells from second pass"
        );
    }

    private OvermapGrid buildLakeGrid() {
        final OvermapGrid grid = new OvermapGrid(32, 32, "test_field");
        BaseTerrainFiller.fill(grid, options, lakeRegion, null, new Random(99L));
        LakeGenerator.fill(grid, options, lakeRegion, null, new Random(99L), new ArrayList<>());
        return grid;
    }

    private static int countRiverCells(final OvermapGrid grid) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if ("test_river".equals(grid.getOmtId(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }
}
