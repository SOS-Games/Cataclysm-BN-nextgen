package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolumeBuilder;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapgenFileBundleInferrerTest {

    @Test
    void infersMultiFloorBundleFromSharedPrefixInSameFile() throws Exception {
        final MapgenCatalog catalog = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        ).getCatalog();

        final CityBuildingRegistry inferred = MapgenFileBundleInferrer.augment(
            CityBuildingRegistry.empty(),
            catalog
        );

        final CityBuildingDefinition cottage = inferred.findById("implicit_cottage").orElseThrow();
        assertEquals(2, cottage.floorCount());
        assertTrue(cottage.isBundledBuilding());
        assertEquals("implicit_cottage_base", cottage.piecesAtZ(0).get(0).getOvermapId());
        assertEquals("implicit_cottage_roof", cottage.piecesAtZ(1).get(0).getOvermapId());
        assertTrue(inferred.getWarnings().stream().anyMatch(w -> w.contains("implicit_cottage")));
    }

    @Test
    void doesNotOverrideExplicitCityBuildingMetadata() throws Exception {
        final MapgenCatalog catalog = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        ).getCatalog();
        final CityBuildingRegistry explicit = CityBuildingLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        );

        final CityBuildingRegistry merged = MapgenFileBundleInferrer.augment(explicit, catalog);

        final CityBuildingDefinition duplex = merged.findById("test_duplex").orElseThrow();
        assertEquals(2, duplex.floorCount());
        assertEquals(1, merged.findById("test_duplex").orElseThrow().piecesAtZ(1).size());
        assertTrue(
            merged.getWarnings().stream().noneMatch(w -> w.contains("inferred implicit bundle 'test_duplex'"))
        );
    }

    @Test
    void inferredBundleBuildsMapVolume() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final CityBuildingDefinition cottage = MapgenFileBundleInferrer.augment(
            CityBuildingRegistry.empty(),
            catalog
        ).findById("implicit_cottage").orElseThrow();

        final MapVolumeBuilder.MapVolumeBuildResult result = MapVolumeBuilder.build(
            cottage,
            catalog,
            PaletteLoader.load(options).getPalettes(),
            null
        );

        assertEquals(2, result.getVolume().floorCount());
        assertEquals(0, result.getVolume().getActiveZ());
        assertEquals(5, result.getVolume().getGridAtZ(0).width());
        assertEquals(3, result.getVolume().getGridAtZ(1).height());
    }
}
