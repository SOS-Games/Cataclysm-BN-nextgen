package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SwampGeneratorTest {

    @Test
    void swampHeavyRegionPaintsMoreSwampThanDefault() throws Exception {
        final RegionSettingsDefinition swampHeavy = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("swamp_heavy").orElseThrow();
        final RegionSettingsDefinition defaultRegion = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("default").orElseThrow();
        final io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry oterRegistry =
            OvermapTerrainLoader.load(
                OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
            ).getRegistry();
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(32, 32)
            .withSeed(55L)
            .withTerrainIds("open_air", "test_field");

        final OvermapGrid heavy = buildWithSwampPass(options, swampHeavy, oterRegistry);
        final OvermapGrid plain = buildWithSwampPass(options, defaultRegion, oterRegistry);

        assertTrue(countOmt(heavy, "test_swamp") > countOmt(plain, "test_swamp"));
    }

    private static OvermapGrid buildWithSwampPass(
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry oterRegistry
    ) {
        final OvermapGrid grid = new OvermapGrid(32, 32, "open_air");
        final Random rng = new Random(options.getSeed() ^ 0x504C4143L);
        BaseTerrainFiller.fill(grid, options, region, oterRegistry, rng);
        LakeGenerator.fill(grid, options, region, oterRegistry, rng, new ArrayList<>());
        RiverGenerator.carve(grid, options, oterRegistry, rng, new ArrayList<>());
        SwampGenerator.fill(grid, options, region, oterRegistry, rng, new ArrayList<>());
        BeachGenerator.paint(grid, options, region, oterRegistry, new ArrayList<>());
        return grid;
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
