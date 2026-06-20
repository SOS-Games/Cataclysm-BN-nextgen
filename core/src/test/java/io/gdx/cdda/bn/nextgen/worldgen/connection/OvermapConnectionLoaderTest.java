package io.gdx.cdda.bn.nextgen.worldgen.connection;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapConnectionLoaderTest {

    @Test
    void loadsFixtureLocalRoadTemplate() throws Exception {
        final OvermapConnectionLoadResult result = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        assertTrue(result.getRegistry().contains("test_local_road"));
        assertTrue(
            result.getRegistry().find("test_local_road")
                .map(def -> "test_road".equals(def.getDefaultTerrain()))
                .orElse(false)
        );
    }
}
