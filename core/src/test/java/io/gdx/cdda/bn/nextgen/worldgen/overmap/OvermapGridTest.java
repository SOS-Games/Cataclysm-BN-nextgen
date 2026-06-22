package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapGridTest {

    @Test
    void loadsFixtureLayout() throws Exception {
        final OvermapGrid grid = OvermapGridFactory.fromJsonFile(WorldgenTestFixtures.overmapFixturePath());

        assertEquals(8, grid.width());
        assertEquals(8, grid.height());
        assertEquals("test_room", grid.getOmtId(1, 1));
        assertEquals("test_duplex_ground_north", grid.getOmtId(4, 4));
        assertEquals("test_multitile_west_north", grid.getOmtId(2, 6));
    }

    @Test
    void emptyGridFillsAllCells() {
        final OvermapGrid grid = OvermapGridFactory.empty(4, 4, "open_air");
        assertEquals("open_air", grid.getOmtId(3, 3));
    }

    @Test
    void largeGridAllocAndRandomAccess() {
        final OvermapGrid grid = OvermapGridFactory.empty(180, 180, "field");
        assertEquals(180, grid.width());
        assertEquals(180, grid.height());
        grid.setOmtId(179, 179, "forest");
        assertEquals("forest", grid.getOmtId(179, 179));
        assertEquals("field", grid.getOmtId(0, 0));
    }

    @Test
    void smokeFromCatalogPlacesVisitableOmts() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        final OvermapTerrainLoadResult oterResult = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );

        final OvermapGrid grid = OvermapGridFactory.smokeFromCatalog(
            oterResult.getRegistry(),
            service.getCatalog()
        );

        assertEquals(8, grid.width());
        assertTrue(OvermapGridFactory.isVisitableOmt(grid.getOmtId(1, 1), service.getCatalog()));
    }
}
