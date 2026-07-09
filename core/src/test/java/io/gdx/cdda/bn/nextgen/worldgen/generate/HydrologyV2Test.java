package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydrologyV2Test {

    @Test
    void riverScaleZeroSkipsCarve() throws Exception {
        final RegionSettingsDefinition noRivers = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("no_rivers").orElseThrow();
        assertEquals(0.0, noRivers.getRiverScale(), 0.0001);

        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(12L)
            .withTerrainIds("test_field", "test_field")
            .withRegionId("no_rivers")
            .withConnectivity(true, false, "local_road", "test_river", "test_river");

        final int carved = RiverGenerator.carve(
            grid,
            options,
            noRivers,
            null,
            new Random(12L),
            new ArrayList<>()
        );
        assertEquals(0, carved);
        assertEquals("test_field", grid.getOmtId(8, 8));
    }

    @Test
    void lakeRegionUsesSurfaceAndShoreIds() throws Exception {
        final RegionSettingsDefinition lakeRegion = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("lake_test").orElseThrow();
        assertEquals("test_lake_surface", lakeRegion.getLakeSettings().getLakeSurfaceOterId());
        assertEquals("test_lake_shore", lakeRegion.getLakeSettings().getLakeShoreOterId());
    }
}
