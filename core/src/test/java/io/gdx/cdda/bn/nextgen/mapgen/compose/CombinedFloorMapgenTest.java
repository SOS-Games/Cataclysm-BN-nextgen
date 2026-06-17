package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedFloorMapgenTest {

    @Test
    void loadsNestedOmTerrainBuildingFromFixture() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("test_nested_omt")
            .orElseThrow();
        final List<CityBuildingPiece> pieces = building.piecesAtZ(0);

        assertTrue(catalog.findByOmTerrain("test_nested_sw").get(0).getOmTerrainGrid().isPresent());
        assertTrue(CombinedFloorMapgenResolver.resolve(catalog, pieces).isPresent());

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertEquals(1, volume.floorCount());
        assertEquals(6, volume.getGridAtZ(0).width());
        assertEquals(4, volume.getGridAtZ(0).height());
        assertEquals(4, volume.getPieceLayoutsAtZ(0).size());
        assertEquals("f_chair", volume.getGridAtZ(0).get(2, 1).getFurnitureId());
    }

    @Test
    void loadsWestFirstOmTerrainRowFromFixture() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("test_west_first_row")
            .orElseThrow();
        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertEquals(12, volume.getGridAtZ(0).width());
        assertEquals(4, volume.getGridAtZ(0).height());
        assertEquals(2, volume.getPieceLayoutsAtZ(0).size());
        assertEquals("t_floor", volume.getGridAtZ(0).get(1, 1).getTerrainId());
        assertEquals("t_floor", volume.getGridAtZ(0).get(7, 1).getTerrainId());
    }
}
