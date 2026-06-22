package io.gdx.cdda.bn.nextgen.worldgen.region;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionSettingsLoaderTest {

    @Test
    void loadsDefaultAndTestRegionsFromFixture() throws Exception {
        final RegionSettingsLoadResult result = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );

        assertTrue(result.getRegistry().find("default").isPresent());
        assertTrue(result.getRegistry().find("forest_heavy").isPresent());
        assertTrue(result.getRegistry().find("forest_light").isPresent());

        final RegionSettingsDefinition heavy = result.getRegistry().find("forest_heavy").orElseThrow();
        assertEquals("open_air", heavy.getDefaultOter());
        assertEquals(0.9, heavy.getForestSettings().getNoiseThresholdForest(), 0.0001);
        assertTrue(heavy.hasCityHouseWeights());
        assertEquals(100, heavy.getCityHouseWeights().get("test_multitile").intValue());

        final RegionSettingsDefinition specialHeavy = result.getRegistry().find("special_heavy").orElseThrow();
        assertTrue(specialHeavy.getSpecialSettings().isEnabled());
        assertEquals(2, specialHeavy.getSpecialSettings().getMinCount());
        assertEquals(4, specialHeavy.getSpecialSettings().getMaxCount());

        final RegionSettingsDefinition sparseCities = result.getRegistry().find("sparse_cities").orElseThrow();
        assertEquals(12, sparseCities.getCitySizeSettings().getCitySpacing());
        assertEquals(3, sparseCities.getCitySizeSettings().getCitySize());
    }
}
