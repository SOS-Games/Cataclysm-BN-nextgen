package io.gdx.cdda.bn.nextgen.mapgen.region;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RegionalTerrainResolverTest {

    @Test
    void loadsTestRegionFromFixture() throws Exception {
        final RegionContext context = RegionContext.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()),
            new ArrayList<>()
        );

        assertTrue(context.regionIds().contains("test_region"));
        assertEquals(
            "t_grass",
            context.resolveTerrain("test_region", "t_region_groundcover", null, null)
        );
        assertEquals(
            "t_dirt",
            context.resolveTerrain("test_region", "t_region_groundcover_barren", null, null)
        );
    }

    @Test
    void fillTerResolvesToConcreteTerrain() throws Exception {
        final MapgenScanOptions scanOptions = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final RegionContext context = RegionContext.load(scanOptions, new ArrayList<>());
        final PaletteRegistry palettes = PaletteLoader.load(scanOptions).getPalettes();
        final JsonMapgenDefinition definition = JsonMapgenLoader.load(scanOptions)
            .getCatalog()
            .findByOmTerrain("test_region_fill")
            .get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewRegionId("test_region")
            .withRegionContext(context);

        final MapGrid grid = JsonMapgenRunner.run(definition, palettes, options);

        assertEquals("t_grass", grid.get(0, 0).getTerrainId());
        assertEquals("t_grass", grid.get(4, 4).getTerrainId());
    }

    @Test
    void setmapRegionalIdResolvesAfterPlacement() throws Exception {
        final MapgenScanOptions scanOptions = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final RegionContext context = RegionContext.load(scanOptions, new ArrayList<>());
        final PaletteRegistry palettes = PaletteLoader.load(scanOptions).getPalettes();
        final JsonMapgenDefinition definition = JsonMapgenLoader.load(scanOptions)
            .getCatalog()
            .findByOmTerrain("test_region_setmap")
            .get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewRegionId("test_region")
            .withRegionContext(context);

        final MapGrid grid = JsonMapgenRunner.run(definition, palettes, options);

        assertEquals("t_grass", grid.get(0, 0).getTerrainId());
        assertEquals("t_dirt", grid.get(1, 1).getTerrainId());
    }

    @Test
    void emptyAliasRegionFallsBackToDefault() throws Exception {
        final RegionContext context = RegionContext.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()),
            new ArrayList<>()
        );
        assertTrue(context.regionIds().contains("forest_trails"));
        assertEquals(
            "t_grass",
            context.resolveTerrain("forest_trails", "t_region_groundcover", null, null)
        );
        assertEquals(
            "t_underbrush",
            context.resolveTerrain("forest_trails", "t_region_shrub", null, null)
        );
    }

    @Test
    void unknownRegionFallsBackToDefaultWithWarning() throws Exception {
        final RegionContext context = RegionContext.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()),
            new ArrayList<>()
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewRegionId("missing_region")
            .withRegionContext(context);

        final MapGrid grid = new MapGrid(1, 1, "t_region_groundcover");
        RegionalTerrainResolver.applyToGrid(grid, context, options);

        assertFalse(options.getWarnings().isEmpty());
        assertTrue(options.getWarnings().get(0).contains("missing_region"));
    }

    @Test
    void integrationLoadsDefaultRegionFromSiblingBn() throws Exception {
        final Path bnData = Paths.get("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(bnData.toFile().isDirectory(), "Cataclysm-BN/data not found beside nextgen");

        final RegionContext context = RegionContext.load(MapgenScanOptions.fromDataRoot(bnData), new ArrayList<>());
        assertTrue(context.regionIds().contains("default"));

        final String resolved = context.resolveTerrain(
            "default",
            "t_region_groundcover",
            new java.util.Random(1L),
            null
        );
        assertNotNull(resolved);
        assertFalse(resolved.startsWith("t_region_"));
    }
}
