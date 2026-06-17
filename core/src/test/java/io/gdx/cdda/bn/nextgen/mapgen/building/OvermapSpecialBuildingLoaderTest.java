package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OvermapSpecialBuildingLoaderTest {

    @Test
    void loadsVerticalStackFromFixtureSpecial() throws Exception {
        final CityBuildingRegistry registry = CityBuildingLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        );

        final CityBuildingDefinition farm = registry.findById("test_farm_6").orElseThrow();
        assertEquals(3, farm.floorCount());
        assertTrue(farm.isBundledBuilding());
        assertEquals("3 floors", farm.buildingSummaryLabel());
    }

    @Test
    void generatesFarmStackBuilding() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()));

        final CityBuildingDefinition farm = service.getCityBuildings()
            .findById("test_farm_6")
            .orElseThrow();

        final MapgenPreviewService.MapgenBuildingResult result = service.generateBuilding(farm, null);
        final MapVolume volume = result.getVolume();

        assertEquals(3, volume.floorCount());
        assertEquals(5, volume.getGridAtZ(0).width());
        assertEquals(3, volume.getGridAtZ(1).width());
    }

    @Test
    void loadsTwoFarmSixFromSiblingBnData() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty());

        final CityBuildingRegistry registry = CityBuildingLoader.load(options);
        final CityBuildingDefinition farm = registry.findById("2farm_6").orElseThrow();

        assertTrue(farm.floorCount() >= 4);
        assertTrue(farm.isBundledBuilding());
        assertTrue(registry.findById("2farm_6").isPresent());
    }
}
