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

        final RegionSettingsDefinition urbanHeavy = result.getRegistry().find("urban_heavy").orElseThrow();
        assertEquals(80, urbanHeavy.getCityContentWeights().getShops().get("test_shop").intValue());
        assertEquals(60, urbanHeavy.getCityContentWeights().getParks().get("test_park").intValue());
        assertEquals(10, urbanHeavy.getCityContentWeights().getFinales().get("test_finale").intValue());

        final RegionSettingsDefinition forestTrails = result.getRegistry().find("forest_trails").orElseThrow();
        assertTrue(forestTrails.getForestTrailSettings().isEnabled());
        assertEquals(20, forestTrails.getForestTrailSettings().getMinimumForestSize());
        assertEquals(1, forestTrails.getForestTrailSettings().getTrailheads().get("test_trailhead").intValue());

        final RegionSettingsDefinition underground = result.getRegistry().find("underground_networks").orElseThrow();
        assertTrue(underground.getUndergroundNetworkSettings().isEnabled());
        assertTrue(underground.getUndergroundNetworkSettings().isSubwaysEnabled());
        assertTrue(underground.getUndergroundNetworkSettings().isSewersEnabled());
    }
}
