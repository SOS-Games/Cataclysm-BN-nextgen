package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalogResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CityBuildingLoaderTest {

    @Test
    void loadsCityBuildingFromFixtureModTree() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalogResult catalogResult = JsonMapgenLoader.load(options);
        final MapgenCatalog catalog = catalogResult.getCatalog();
        final CityBuildingRegistry registry = CityBuildingLoader.load(options).withOmTerrainIndex(catalog);

        final CityBuildingDefinition duplex = registry.findById("test_duplex").orElseThrow();
        assertEquals(2, duplex.floorCount());
        assertEquals(0, duplex.piecesAtZ(0).get(0).getZLevel());
        assertEquals("test_duplex_ground_north", duplex.piecesAtZ(0).get(0).getOvermapId());
        assertTrue(registry.findByOmTerrain("test_duplex_ground").isPresent());
        assertTrue(registry.findByOmTerrain("test_duplex_roof").isPresent());

        final CityBuildingDefinition multitile = registry.findById("test_multitile").orElseThrow();
        assertTrue(multitile.hasMultiTileLayout());
        assertEquals("multi-tile 2×1", multitile.buildingSummaryLabel());
        assertTrue(registry.findByOmTerrain("test_multitile_west").isPresent());
    }
}
