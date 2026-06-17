package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapVolumeBuilderTest {

    @Test
    void buildsDuplexFloorsFromFixture() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()));

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("test_duplex")
            .orElseThrow();

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertEquals(2, volume.floorCount());
        assertEquals(0, volume.getActiveZ());
        assertEquals(5, volume.getGridAtZ(0).width());
        assertEquals("f_chair", volume.getGridAtZ(0).get(2, 2).getFurnitureId());

        volume.setActiveZ(1);
        assertEquals(3, volume.getActiveGrid().width());
        assertEquals("t_wall", volume.getActiveGrid().get(1, 1).getTerrainId());
        assertNotEquals(
            volume.getGridAtZ(0).get(2, 2).getTerrainId(),
            volume.getGridAtZ(1).get(1, 1).getTerrainId()
        );
    }

    @Test
    void stitchesMultitileBuildingFromFixture() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()));

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("test_multitile")
            .orElseThrow();
        assertTrue(building.hasMultiTileLayout());
        assertTrue(building.isBundledBuilding());
        assertEquals("multi-tile 2×1", building.omtFootprintLabel());
        assertEquals("multi-tile 2×1", building.buildingSummaryLabel());

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertEquals(1, volume.floorCount());
        final MapGrid grid = volume.getGridAtZ(0);
        assertEquals(OmtStitchComposer.DEFAULT_OMT_SIZE + 5, grid.width());
        assertEquals("f_chair", grid.get(2, 2).getFurnitureId());
        assertEquals("t_wall", grid.get(OmtStitchComposer.DEFAULT_OMT_SIZE + 2, 2).getTerrainId());

        final List<OmtPieceRect> layouts = volume.getPieceLayoutsAtZ(0);
        assertEquals(2, layouts.size());
        assertEquals(0, layouts.get(0).getOriginX());
        assertEquals(OmtStitchComposer.DEFAULT_OMT_SIZE, layouts.get(1).getOriginX());
    }
}
