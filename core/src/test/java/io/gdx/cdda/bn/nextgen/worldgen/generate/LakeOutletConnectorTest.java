package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LakeOutletConnectorTest {

    @Test
    void connectsLakeToNearestRiver() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final RegionSettingsDefinition region = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("lake_test").orElseThrow();
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(32, 32)
            .withSeed(99L)
            .withRegionId("lake_test")
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, true, "test_local_road", "test_river", "test_river")
            .withLakesEnabled(true);

        final OvermapGrid grid = new OvermapGrid(32, 32, "test_field");
        for (int y = 12; y <= 18; y++) {
            for (int x = 10; x <= 16; x++) {
                grid.setOmtId(x, y, "test_lake_surface");
            }
        }
        for (int y = 0; y < grid.height(); y++) {
            grid.setOmtId(28, y, "test_river");
        }

        final int painted = LakeOutletConnector.connectAll(
            grid,
            options,
            region,
            registry,
            new Random(99L)
        );
        assertTrue(painted > 0, "expected outlet carve to paint river cells");

        int riverBetween = 0;
        for (int x = 17; x < 28; x++) {
            for (int y = 10; y <= 20; y++) {
                if (HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, y), options, registry)) {
                    riverBetween++;
                }
            }
        }
        assertTrue(riverBetween > 0, "expected river path between lake and east river");
    }
}
