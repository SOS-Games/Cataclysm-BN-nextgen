package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BuildingBundleScannerTest {

    @Test
    void loadsCityBuildingFromNestedOvermapPath() throws Exception {
        final CityBuildingRegistry registry = CityBuildingLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        );

        final CityBuildingDefinition building = registry.findById("test_nested_overmap_path").orElseThrow();
        assertTrue(building.hasMultiTileLayout());
        assertEquals(2, building.piecesAtZ(0).size());
    }

    @Test
    void loadsBundlesFromNonStandardModPaths() throws Exception {
        final MapgenScanOptions options = new MapgenScanOptions(
            Arrays.asList(MapgenTestFixtures.fixtureDataRoot()),
            Arrays.asList("bn", "overlay_mod"),
            true,
            true,
            false
        );
        final CityBuildingRegistry registry = CityBuildingLoader.load(options);

        final CityBuildingDefinition regional = registry.findById("test_regional_house").orElseThrow();
        assertEquals(2, regional.floorCount());
        assertTrue(regional.isBundledBuilding());

        final CityBuildingDefinition stack = registry.findById("test_mod_root_base").orElseThrow();
        assertEquals(3, stack.floorCount());
        assertEquals("3 floors", stack.buildingSummaryLabel());
    }

    @Test
    void loadsHouseArcanaFromSiblingBnWhenArcanaModPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final CityBuildingRegistry registry = CityBuildingLoader.load(
            new MapgenScanOptions(
                options.getDataRoots(),
                Arrays.asList("bn", "Arcana"),
                options.isIncludePaletteTree(),
                options.isIncludeMapgenTree(),
                options.isIncludeInlinePalettes()
            )
        );

        assumeTrue(registry.findById("house_arcana").isPresent(), "Arcana mod not installed");
        final CityBuildingDefinition arcana = registry.findById("house_arcana").orElseThrow();
        assertEquals(3, arcana.floorCount());
        assertTrue(arcana.isBundledBuilding());
    }
}
