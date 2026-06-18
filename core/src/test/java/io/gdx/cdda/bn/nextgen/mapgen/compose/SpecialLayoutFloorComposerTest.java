package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SpecialLayoutFloorComposerTest {

    @Test
    void registersWholeSpecialBundle() throws Exception {
        final CityBuildingDefinition building = CityBuildingLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        ).findById("test_special_wide").orElseThrow();

        assertTrue(building.isWholeOvermapSpecial());
        assertTrue(building.hasMultiTileLayout());
        assertEquals(4, building.getPieces().size());
    }

    @Test
    void composesUpperFloorsAtGroundAnchors() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()));

        final CityBuildingDefinition building = service.getCityBuildings()
            .findById("test_special_wide")
            .orElseThrow();

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(building, null);
        final MapVolume volume = result.getVolume();

        assertEquals(2, volume.floorCount());
        final MapGrid ground = volume.getGridAtZ(0);
        final MapGrid upper = volume.getGridAtZ(1);
        assertEquals(ground.width(), upper.width());
        assertEquals(ground.height(), upper.height());
        assertEquals("t_wall", ground.get(0, 0).getTerrainId());
        assertEquals("t_dirt", ground.get(24, 0).getTerrainId());
        assertEquals("t_shingle_flat_roof", upper.get(0, 0).getTerrainId());
        assertEquals("t_dirt", upper.get(24, 0).getTerrainId());
        assertNotEquals(ground.getDefaultTerrainId(), upper.get(0, 0).getTerrainId());
    }

    @Test
    void farm2sideShowsSiloOnUpperFloorWhenSiblingBnPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final CityBuildingDefinition building = CityBuildingLoader.load(options)
            .findById("Farm_2side")
            .orElseThrow();
        assumeTrue(building.isWholeOvermapSpecial());

        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);
        final MapVolume volume = service.generateBuilding(building, null).getVolume();

        final MapGrid ground = volume.getGridAtZ(0);
        final MapGrid upper = volume.getGridAtZ(1);
        assertEquals(96, ground.width());
        assertEquals(96, upper.width());

        boolean siloOnGround = false;
        boolean siloOnUpper = false;
        for (int y = 24; y < 48; y++) {
            for (int x = 48; x < 72; x++) {
                if ("t_wall_metal".equals(ground.get(x, y).getTerrainId())) {
                    siloOnGround = true;
                }
                if ("t_wall_metal".equals(upper.get(x, y).getTerrainId())) {
                    siloOnUpper = true;
                }
            }
        }
        assertTrue(siloOnGround, "expected silo metal walls on ground near 2farm_6");
        assertTrue(siloOnUpper, "expected silo metal walls on z=1 near 2farm_6");
    }

    @Test
    void microlabArcanaImportsWhenArcanaModPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final CityBuildingDefinition building = CityBuildingLoader.load(options)
            .findById("4x4_microlab_arcana")
            .orElse(null);
        assumeTrue(building != null, "Arcana mod not loaded");

        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(options);
        final MapVolume volume = service.generateBuilding(building, null).getVolume();

        assertTrue(volume.floorCount() >= 1);
        assertTrue(volume.getActiveGrid().width() > 0);
    }
}
