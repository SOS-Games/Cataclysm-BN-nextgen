package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

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
}
