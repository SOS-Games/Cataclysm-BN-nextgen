package io.gdx.cdda.bn.nextgen.worldgen.region;

import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RegionSettingsOverlayIntegrationTest {

    @Test
    void loadsForestTrailsPreviewRegionFromNextgenOverlay() throws Exception {
        final Path overlay = Paths.get("").toAbsolutePath().resolve("data").normalize();
        assumeTrue(Files.isDirectory(overlay.resolve("json/region_settings")));

        final String previous = System.getProperty(DataPaths.DATA_ROOTS_PROPERTY);
        System.clearProperty(DataPaths.DATA_ROOTS_PROPERTY);
        try {
            final RegionSettingsLoadResult result = RegionSettingsLoader.load(MapgenScanOptions.defaults());
            assertTrue(
                result.getRegistry().find("forest_trails").isPresent(),
                "expected forest_trails from data/json/region_settings/worldgen_preview_regions.json"
            );
        } finally {
            if (previous == null) {
                System.clearProperty(DataPaths.DATA_ROOTS_PROPERTY);
            } else {
                System.setProperty(DataPaths.DATA_ROOTS_PROPERTY, previous);
            }
        }
    }
}
