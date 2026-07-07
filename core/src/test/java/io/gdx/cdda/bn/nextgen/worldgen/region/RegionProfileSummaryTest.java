package io.gdx.cdda.bn.nextgen.worldgen.region;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionProfileSummaryTest {

    @Test
    void previewProfilesSortFirstInPicker() throws Exception {
        final RegionSettingsRegistry registry = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();

        final java.util.List<String> picker = registry.regionIdsForPicker();
        assertTrue(picker.indexOf("underground_networks") < picker.indexOf("default"));
        assertTrue(picker.indexOf("forest_trails") < picker.indexOf("default"));
    }

    @Test
    void describesForestTrailsProfile() throws Exception {
        final RegionSettingsDefinition region = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("forest_trails").orElseThrow();

        final String summary = RegionProfileSummary.describe(region);
        assertTrue(summary.contains("trails"));
        assertTrue(summary.contains("houses") || summary.contains("city"));
    }

    @Test
    void marksPreviewProfileIds() {
        assertTrue(RegionProfileSummary.isLayoutPreviewProfile("preview_urban"));
        assertTrue(RegionProfileSummary.isLayoutPreviewProfile("forest_trails"));
        assertEquals(false, RegionProfileSummary.isLayoutPreviewProfile("default"));
    }
}
