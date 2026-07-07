package io.gdx.cdda.bn.nextgen.worldgen;

import io.gdx.cdda.bn.nextgen.worldgen.region.CitySizeSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldgenWorldOptionsTest {

    @Test
    void bnDefaultsMatchNewWorldCityOptions() {
        final WorldgenWorldOptions options = WorldgenWorldOptions.bnDefaults();
        assertEquals(8, options.getCitySize());
        assertEquals(4, options.getCitySpacing());
    }

    @Test
    void regionCitySizeFallsBackToWorldOptionsWhenOmitted() throws Exception {
        final CitySizeSettings raw = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("bn_default_city").orElseThrow().getCitySizeSettings();

        assertEquals(CitySizeSettings.USE_WORLD_OPTION, raw.getRawCitySize());
        assertEquals(CitySizeSettings.USE_WORLD_OPTION, raw.getRawCitySpacing());
        assertFalse(raw.isEnabled());

        final CitySizeSettings resolved = raw.resolve(WorldgenWorldOptions.bnDefaults());
        assertEquals(8, resolved.getCitySize());
        assertEquals(4, resolved.getCitySpacing());
        assertTrue(resolved.isEnabled());
    }

    @Test
    void explicitRegionCitySizeOverridesWorldOptions() throws Exception {
        final CitySizeSettings raw = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("sparse_cities").orElseThrow().getCitySizeSettings();

        final CitySizeSettings resolved = raw.resolve(WorldgenWorldOptions.bnDefaults());
        assertEquals(3, resolved.getCitySize());
        assertEquals(12, resolved.getCitySpacing());
    }

    @Test
    void noCitiesWorldOptionDisablesUrbanBlobs() throws Exception {
        final CitySizeSettings raw = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("bn_default_city").orElseThrow().getCitySizeSettings();

        assertFalse(raw.resolve(WorldgenWorldOptions.noCities()).isEnabled());
    }
}
