package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LakeGeneratorTest {

    private RegionSettingsDefinition lakeRegion;

    @BeforeEach
    void loadRegion() throws Exception {
        final RegionSettingsRegistry registry = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        lakeRegion = registry.find("lake_test").orElseThrow();
    }

    @Test
    void paintsLakeClusterOn32x32() {
        final OvermapGrid grid = new OvermapGrid(32, 32, "test_field");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(32, 32)
            .withSeed(77L)
            .withTerrainIds("test_field", "test_field")
            .withRegionId("lake_test")
            .withLakesEnabled(true);
        final int painted = LakeGenerator.fill(
            grid,
            options,
            lakeRegion,
            null,
            new Random(77L),
            new ArrayList<String>()
        );
        assertTrue(painted >= 4, "expected lake cluster, painted=" + painted);

        boolean foundSurface = false;
        boolean foundShore = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if ("test_lake_surface".equals(id)) {
                    foundSurface = true;
                }
                if ("test_lake_shore".equals(id)) {
                    foundShore = true;
                }
            }
        }
        assertTrue(foundSurface || foundShore, "expected lake surface or shore OMT ids");
    }
}
