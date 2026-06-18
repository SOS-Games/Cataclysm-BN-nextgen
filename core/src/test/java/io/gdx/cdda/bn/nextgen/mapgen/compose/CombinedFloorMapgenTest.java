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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

    @Test
    void stitchesFloorWithNegativeOriginOffsets() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("test_negative_origin")
            .orElseThrow();
        final List<CityBuildingPiece> pieces = building.piecesAtZ(0);

        assertTrue(CombinedFloorMapgenResolver.resolve(catalog, pieces).isPresent());

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertEquals(48, volume.getGridAtZ(0).width());
        assertEquals("t_wall", volume.getGridAtZ(0).get(0, 0).getTerrainId());
        assertEquals("t_dirt", volume.getGridAtZ(0).get(24, 0).getTerrainId());
    }

    @Test
    void imports2fMotelWholeSpecialFromSiblingBnWhenPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);
        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("2fMotel")
            .orElseThrow();
        assumeTrue(building.isWholeOvermapSpecial());

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertTrue(volume.floorCount() >= 2);
        assertTrue(volume.getGridAtZ(0).width() > 0);
        assertTrue(volume.getGridAtZ(0).height() > 0);
    }
}
