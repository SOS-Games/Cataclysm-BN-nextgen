package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ApartmentsConNewIntegrationTest {

    @Test
    void loadsApartmentsConNewFromSiblingBnData() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("apartments_con_new")
            .orElseThrow();

        assertTrue(building.hasMultiTileLayout());
        assertTrue(building.floorCount() >= 6);

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertTrue(volume.floorCount() >= 6, "expected multiple floors, got " + volume.floorCount());
        assertTrue(volume.getGridAtZ(0).width() > 24, "ground floor should be wider than one OMT");
        assertEquals(4, volume.getPieceLayoutsAtZ(0).size());
    }
}
