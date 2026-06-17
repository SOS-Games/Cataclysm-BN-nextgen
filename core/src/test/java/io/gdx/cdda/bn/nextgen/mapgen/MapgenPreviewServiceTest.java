package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapgenPreviewServiceTest {

    @Test
    void loadsCatalogAndGeneratesFixtureRoom() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()));

        final MapgenCatalog catalog = service.getCatalog();
        assertFalse(catalog.runnableOnly().isEmpty());

        final JsonMapgenDefinition definition = catalog.findByOmTerrain("test_room").get(0);
        final MapgenPreviewService.MapgenPreviewResult result = service.generate(
            definition,
            null,
            null
        );

        assertEquals(5, result.getGrid().width());
        assertEquals(5, result.getGrid().height());
        assertEquals("t_wall", result.getGrid().get(0, 0).getTerrainId());
        assertEquals("f_chair", result.getGrid().get(2, 2).getFurnitureId());
        assertTrue(service.getLoadWarnings().isEmpty() || service.isLoaded());
    }
}
