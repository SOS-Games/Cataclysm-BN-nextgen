package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingLoader;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.OmTerrainGrid;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OmTerrainMapgenPlacerTest {

    @Test
    void placesStripMapgenAtWestOnSharedCanvas() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final JsonMapgenDefinition definition = catalog.findFirstRunnableByOmTerrain("test_crop_west").orElseThrow();
        final PaletteRegistry palettes = PaletteLoader.load(options).getPalettes();
        final OmTerrainGrid stripGrid = definition.getOmTerrainGrid().orElseThrow();

        final MapGrid source = JsonMapgenRunner.run(definition, palettes, new JsonMapgenRunOptions());
        final OmTerrainGrid referenceGrid = new OmTerrainGrid(
            Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d")
            )
        );

        final MapGrid placed = OmTerrainMapgenPlacer.placeOnCanvas(
            source,
            stripGrid,
            "test_crop_west",
            referenceGrid,
            0,
            0,
            48,
            6,
            source.getDefaultTerrainId()
        );

        assertEquals(48, placed.width());
        assertEquals(6, placed.height());
        assertEquals("t_wall", placed.get(0, 0).getTerrainId());
        assertEquals("t_wall", placed.get(23, 2).getTerrainId());
        assertEquals("t_dirt", placed.get(47, 0).getTerrainId());
        assertEquals(source.getDefaultTerrainId(), placed.get(0, 5).getTerrainId());
    }

    @Test
    void multiFloorBuildingAlignsUpperFloorsToGroundCanvas() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final PaletteRegistry palettes = PaletteLoader.load(options).getPalettes();

        final CityBuildingDefinition building = new CityBuildingDefinition(
            "test_crop_duplex",
            null,
            Arrays.asList(
                new CityBuildingPiece(0, 0, 0, "test_crop_west"),
                new CityBuildingPiece(0, 0, 1, "test_crop_east")
            )
        );

        final MapVolumeBuilder.MapVolumeBuildResult result = MapVolumeBuilder.build(
            building,
            catalog,
            palettes,
            new JsonMapgenRunOptions()
        );

        final MapGrid ground = result.getVolume().getGridAtZ(0);
        final MapGrid upper = result.getVolume().getGridAtZ(1);
        assertEquals(ground.width(), upper.width());
        assertEquals(ground.height(), upper.height());
        assertEquals("t_wall", upper.get(24, 0).getTerrainId());
        assertNotEquals(ground.getDefaultTerrainId(), upper.get(24, 0).getTerrainId());
    }

    @Test
    void farm2farm4FloorsShareGroundCanvasWhenSiblingBnPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final CityBuildingRegistry registry = CityBuildingLoader.load(options);
        assumeTrue(registry.findById("2farm_4").isPresent(), "2farm_4 bundle not in BN data");

        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final PaletteRegistry palettes = PaletteLoader.load(options).getPalettes();
        final CityBuildingDefinition farm = registry.findById("2farm_4").orElseThrow();

        final MapVolumeBuilder.MapVolumeBuildResult result = MapVolumeBuilder.build(
            farm,
            catalog,
            palettes,
            new JsonMapgenRunOptions()
        );
        final MapGrid ground = result.getVolume().getGridAtZ(0);
        for (final int z : result.getVolume().getZLevels()) {
            if (z == 0) {
                continue;
            }
            final MapGrid floor = result.getVolume().getGridAtZ(z);
            assertEquals(ground.width(), floor.width(), "z=" + z + " width");
            assertEquals(ground.height(), floor.height(), "z=" + z + " height");
        }
    }

    @Test
    void farm2farm6SiloAlignsToGroundQuadrantWhenSiblingBnPresent() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.defaults();
        assumeTrue(!options.getDataRoots().isEmpty(), "no BN data roots configured");

        final CityBuildingRegistry registry = CityBuildingLoader.load(options);
        assumeTrue(registry.findById("2farm_6").isPresent(), "2farm_6 bundle not in BN data");

        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final PaletteRegistry palettes = PaletteLoader.load(options).getPalettes();
        final CityBuildingDefinition farm = registry.findById("2farm_6").orElseThrow();

        final MapVolumeBuilder.MapVolumeBuildResult result = MapVolumeBuilder.build(
            farm,
            catalog,
            palettes,
            new JsonMapgenRunOptions()
        );
        final MapGrid ground = result.getVolume().getGridAtZ(0);
        final MapGrid upper = result.getVolume().getGridAtZ(1);

        int groundMetalX = -1;
        int groundMetalY = -1;
        int upperMetalX = -1;
        int upperMetalY = -1;
        for (int y = 0; y < ground.height(); y++) {
            for (int x = 0; x < ground.width(); x++) {
                if ("t_wall_metal".equals(ground.get(x, y).getTerrainId())) {
                    if (groundMetalX < 0 || x < groundMetalX) {
                        groundMetalX = x;
                        groundMetalY = y;
                    }
                }
                if ("t_wall_metal".equals(upper.get(x, y).getTerrainId())) {
                    if (upperMetalX < 0 || x < upperMetalX) {
                        upperMetalX = x;
                        upperMetalY = y;
                    }
                }
            }
        }
        assumeTrue(groundMetalX >= 48, "expected silo on ground in 2farm_6 quadrant (x>=48)");
        assertEquals(groundMetalX, upperMetalX, "silo upper floor should align horizontally with ground");
        assertEquals(groundMetalY, upperMetalY, "silo upper floor should align vertically with ground");
    }
}
