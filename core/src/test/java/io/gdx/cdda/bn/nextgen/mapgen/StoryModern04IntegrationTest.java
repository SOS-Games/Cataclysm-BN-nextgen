package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StoryModern04IntegrationTest {

    @Test
    void loads2StoryModern04AsTwoOmtWideCombinedFloor() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("2StoryModern04")
            .orElseThrow();

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertEquals(48, volume.getGridAtZ(0).width(), "ground floor should be 2 OMT wide, not stitched duplicates");
        assertEquals(24, volume.getGridAtZ(0).height());
        assertEquals(2, volume.getPieceLayoutsAtZ(0).size());
        assertTrue(volume.floorCount() >= 4);
    }
}
