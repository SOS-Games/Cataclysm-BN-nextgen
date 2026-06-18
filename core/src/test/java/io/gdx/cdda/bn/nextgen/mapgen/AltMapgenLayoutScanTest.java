package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingLoader;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AltMapgenLayoutScanTest {

    @Test
    void loadsMapgenAndPalettesFromOvermapAndMapgenDir() throws Exception {
        final Path root = MapgenTestFixtures.fixtureDataRoot();
        final MapgenScanOptions options = new MapgenScanOptions(
            List.of(root),
            List.of("bn", "alt_mapgen_mod"),
            true,
            true,
            false
        );

        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        assertTrue(catalog.findFirstRunnableByOmTerrain("alt_layout_ground").isPresent());
        assertTrue(PaletteLoader.load(options).getPalettes().contains("alt_layout_palette"));

        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);
        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("alt_layout_duplex")
            .orElseThrow();

        final MapVolume volume = service.generateBuilding(building, null).getVolume();
        assertEquals(2, volume.floorCount());
        assertEquals("t_wall", volume.getGridAtZ(0).get(1, 1).getTerrainId());
        assertEquals("t_shingle_flat_roof", volume.getGridAtZ(1).get(1, 1).getTerrainId());
    }

    @Test
    void arcanaAnomalyResurgenceImportsWhenArcanaModPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final CityBuildingDefinition building = CityBuildingLoader.load(options)
            .findById("arcana_anomaly_resurgence")
            .orElse(null);
        assumeTrue(building != null, "Arcana mod not loaded");

        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);
        final MapgenCatalog catalog = service.getCatalog();
        assertTrue(
            catalog.findFirstRunnableByOmTerrain("arcana_structure_anomalous_entrance").isPresent(),
            "expected Arcana mapgen under overmap_and_mapgen/"
        );

        final MapVolume volume = service.generateBuilding(building, null).getVolume();
        assertTrue(volume.floorCount() >= 1);
        assertTrue(volume.getActiveGrid().width() > 0);
    }
}
