package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingLoader;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MapgenPickerIndexTest {

    @Test
    void hidesNestedOnlyMapgenFragments() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final CityBuildingRegistry buildings = CityBuildingLoader.load(options);
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final MapgenPickerIndex index = MapgenPickerIndex.build(catalog, buildings);

        assertFalse(
            index.all().stream().anyMatch(row -> !row.isWholeSpecialRow()
                && row.getDefinition()
                    .map(def -> "test_nested_room".equals(def.displayName()))
                    .orElse(false)),
            "nested-only chunk should not appear as a standalone picker row"
        );
        assertTrue(
            index.all().stream().anyMatch(row -> !row.isWholeSpecialRow()
                && row.getDefinition()
                    .map(def -> def.getOmTerrain().contains("test_nested_parent"))
                    .orElse(false)),
            "parent OMT mapgen should still appear in picker"
        );
    }

    @Test
    void includesWholeOvermapSpecialInFixtureIndex() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final CityBuildingRegistry buildings = CityBuildingLoader.load(options);
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final MapgenPickerIndex index = MapgenPickerIndex.build(catalog, buildings);

        assertTrue(
            index.all().stream().anyMatch(row -> row.isWholeSpecialRow()
                && row.getWholeSpecial().map(special -> "test_special_wide".equals(special.getId())).orElse(false)),
            "expected whole special row for test_special_wide"
        );
    }

    @Test
    void hidesColumnStackChunksCoveredByWholeSpecialInFixture() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final CityBuildingRegistry buildings = CityBuildingLoader.load(options);
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final MapgenPickerIndex index = MapgenPickerIndex.build(catalog, buildings);

        assertTrue(buildings.findById("test_farm_stack").isPresent());
        assertTrue(buildings.findById("test_farm_6").isPresent());

        assertFalse(
            index.all().stream().anyMatch(row -> !row.isWholeSpecialRow()
                && row.bundledBuilding(buildings).map(bundle -> "test_farm_6".equals(bundle.getId())).orElse(false)),
            "column stack test_farm_6 should be hidden when test_farm_stack whole special exists"
        );
        assertTrue(
            index.filter("test_farm_6", catalog).stream().anyMatch(row -> row.isWholeSpecialRow()
                && row.getWholeSpecial().map(special -> "test_farm_stack".equals(special.getId())).orElse(false)),
            "filtering by chunk id should still find the parent whole special"
        );
    }

    @Test
    void includesFarm2sideWhenSiblingBnPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final CityBuildingRegistry buildings = CityBuildingLoader.load(options);
        assumeTrue(buildings.findById("Farm_2side").isPresent(), "Farm_2side bundle not in BN data");

        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final MapgenPickerIndex index = MapgenPickerIndex.build(catalog, buildings);

        assertTrue(
            index.filter("Farm_2side", catalog).stream().anyMatch(row -> row.isWholeSpecialRow()
                && row.getWholeSpecial().map(special -> "Farm_2side".equals(special.getId())).orElse(false)),
            "expected Farm_2side whole special row in picker index"
        );
        assertFalse(
            index.all().stream().anyMatch(row -> !row.isWholeSpecialRow()
                && row.bundledBuilding(buildings).map(bundle -> "2farm_6".equals(bundle.getId())).orElse(false)),
            "2farm_6 column stack should be hidden when Farm_2side whole special exists"
        );
        assertFalse(
            index.all().stream().anyMatch(row -> !row.isWholeSpecialRow()
                && row.bundledBuilding(buildings).map(bundle -> "2farm_4".equals(bundle.getId())).orElse(false)),
            "2farm_4 column stack should be hidden when Farm_2side whole special exists"
        );
    }
}
