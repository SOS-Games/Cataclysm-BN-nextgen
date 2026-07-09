package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseTerrainFillerRegionTest {

    private OvermapTerrainRegistry oterRegistry;
    private io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition forestHeavy;

    @BeforeEach
    void loadFixtures() throws Exception {
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry regions = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        forestHeavy = regions.find("forest_heavy").orElseThrow();
    }

    @Test
    void sameSeedIsStableForRegionFill() {
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(4242L)
            .withTerrainIds("open_air", "test_field");
        final OvermapGrid first = new OvermapGrid(16, 16, "open_air");
        final OvermapGrid second = new OvermapGrid(16, 16, "open_air");
        BaseTerrainFiller.fill(first, options, forestHeavy, oterRegistry, null);
        BaseTerrainFiller.fill(second, options, forestHeavy, oterRegistry, null);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                assertEquals(first.getOmtId(x, y), second.getOmtId(x, y));
            }
        }
    }

    @Test
    void regionFillPaintsOnlyDefaultOter() {
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(4242L)
            .withTerrainIds("open_air", "test_field");
        final OvermapGrid grid = new OvermapGrid(16, 16, "open_air");
        BaseTerrainFiller.fill(grid, options, forestHeavy, oterRegistry, null);
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                assertEquals("open_air", grid.getOmtId(x, y));
            }
        }
    }
}
