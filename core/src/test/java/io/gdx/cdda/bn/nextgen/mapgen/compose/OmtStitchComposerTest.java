package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OmtStitchComposerTest {

    @Test
    void stitchesTwoWingsHorizontally() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()));

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("test_multitile")
            .orElseThrow();
        final List<CityBuildingPiece> pieces = building.piecesAtZ(0);
        assertTrue(OmtStitchComposer.needsStitch(pieces));

        final MapgenScanOptions scanOptions = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = service.getCatalog();
        final PaletteRegistry palettes = PaletteLoader.load(scanOptions).getPalettes();
        final OmtStitchComposer.StitchResult result = OmtStitchComposer.stitch(
            pieces,
            catalog,
            palettes,
            new JsonMapgenRunOptions()
        );

        assertTrue(result.getGrid().isPresent());
        final MapGrid grid = result.getGrid().get();
        assertEquals(OmtStitchComposer.DEFAULT_OMT_SIZE + 5, grid.width());
        assertEquals(5, grid.height());
        assertEquals("f_chair", grid.get(2, 2).getFurnitureId());
        assertEquals("t_wall", grid.get(OmtStitchComposer.DEFAULT_OMT_SIZE + 2, 2).getTerrainId());
    }

    @Test
    void cropsSharedMultitileMapgenPerOmtSlot() throws Exception {
        final MapgenScanOptions scanOptions = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(scanOptions).getCatalog();
        final PaletteRegistry palettes = PaletteLoader.load(scanOptions).getPalettes();
        final List<CityBuildingPiece> pieces = Arrays.asList(
            new CityBuildingPiece(0, 0, 0, "test_crop_west"),
            new CityBuildingPiece(1, 0, 0, "test_crop_east")
        );

        final OmtStitchComposer.StitchResult result = OmtStitchComposer.stitch(
            pieces,
            catalog,
            palettes,
            new JsonMapgenRunOptions()
        );

        assertTrue(result.getGrid().isPresent());
        final MapGrid grid = result.getGrid().get();
        assertEquals(48, grid.width());
        assertEquals("t_wall", grid.get(0, 0).getTerrainId());
        assertEquals("t_dirt", grid.get(24, 0).getTerrainId());
        assertEquals(2, result.getPieceRects().size());
        assertEquals(24, result.getPieceRects().get(0).getWidth());
        assertEquals(24, result.getPieceRects().get(1).getWidth());
    }

    @Test
    void stitchMergesSpawnMarkersPerOmtSlot() throws Exception {
        final MapgenScanOptions scanOptions = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(scanOptions).getCatalog();
        final PaletteRegistry palettes = PaletteLoader.load(scanOptions).getPalettes();
        final List<CityBuildingPiece> pieces = Arrays.asList(
            new CityBuildingPiece(0, 0, 0, "test_crop_west"),
            new CityBuildingPiece(1, 0, 0, "test_crop_east")
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions();

        OmtStitchComposer.stitch(pieces, catalog, palettes, options);

        assertEquals(2, options.getSpawnMarkers().size());
        assertEquals("mon_west_bot", options.getSpawnMarkers().get(0).groupId);
        assertEquals(0, options.getSpawnMarkers().get(0).x);
        assertEquals("mon_east_bot", options.getSpawnMarkers().get(1).groupId);
        assertEquals(24, options.getSpawnMarkers().get(1).x);
    }
}
