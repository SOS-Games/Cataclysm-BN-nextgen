package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionGroundcoverSettings;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundOmtSubmapBuilderTest {

    @Test
    void buildsGrassField() {
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "field",
            1L,
            null,
            null,
            RegionGroundcoverSettings.single("t_grass")
        );
        assertTrue(grid.isPresent());
        assertEquals("t_grass", grid.get().get(0, 0).getTerrainId());
    }

    @Test
    void usesRegionGroundcoverForFieldFallback() {
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "field",
            2L,
            null,
            null,
            RegionGroundcoverSettings.single("t_dirt")
        );
        assertTrue(grid.isPresent());
        assertEquals("t_dirt", grid.get().get(0, 0).getTerrainId());
    }

    @Test
    void visitResolverResolvesRegionalGroundcoverAlias() throws Exception {
        final RegionContext regionContext = RegionContext.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()),
            new ArrayList<>()
        );
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "field",
            3L,
            null,
            null,
            RegionGroundcoverSettings.single("t_region_groundcover")
        );
        assertTrue(grid.isPresent());
        assertEquals("t_region_groundcover", grid.get().get(0, 0).getTerrainId());

        final ArrayList<String> warnings = new ArrayList<>();
        VisitRegionalResolver.applyToGrid(grid.get(), regionContext, "test_region", 3L, warnings);
        assertEquals("t_grass", grid.get().get(0, 0).getTerrainId());
        assertFalse(VisitRegionalResolver.hasUnresolvedRegionalIds(grid.get()));
    }

    @Test
    void usesWeightedGroundcoverAcrossFieldCells() {
        final RegionGroundcoverSettings groundcover = RegionGroundcoverSettings.parse(
            new com.badlogic.gdx.utils.JsonReader().parse("[[\"t_dirt\", 3], [\"t_grass\", 1]]")
        );
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "field",
            4242L,
            null,
            null,
            groundcover
        );
        assertTrue(grid.isPresent());
        boolean sawDirt = false;
        boolean sawGrass = false;
        for (int y = 0; y < grid.get().height(); y++) {
            for (int x = 0; x < grid.get().width(); x++) {
                final String ter = grid.get().get(x, y).getTerrainId();
                if ("t_dirt".equals(ter)) {
                    sawDirt = true;
                } else if ("t_grass".equals(ter)) {
                    sawGrass = true;
                }
            }
        }
        assertTrue(sawDirt);
        assertTrue(sawGrass);
    }

    @Test
    void buildsForestWithTrees() throws Exception {
        final RegionContext regionContext = RegionContext.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()),
            new ArrayList<>()
        );
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "forest",
            99L,
            null,
            null,
            RegionGroundcoverSettings.single("t_grass")
        );
        assertTrue(grid.isPresent());
        VisitRegionalResolver.applyToGrid(grid.get(), regionContext, "test_region", 99L, new ArrayList<>());

        boolean sawTree = false;
        for (int y = 0; y < grid.get().height(); y++) {
            for (int x = 0; x < grid.get().width(); x++) {
                final String ter = grid.get().get(x, y).getTerrainId();
                if ("t_tree".equals(ter)) {
                    sawTree = true;
                }
            }
        }
        assertTrue(sawTree);
        assertFalse(VisitRegionalResolver.hasUnresolvedRegionalIds(grid.get()));
    }

    @Test
    void buildsSwampWithShallowWater() {
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "forest_water",
            123L,
            null,
            null,
            RegionGroundcoverSettings.single("t_grass")
        );
        assertTrue(grid.isPresent());
        boolean sawShallowWater = false;
        for (int y = 0; y < grid.get().height(); y++) {
            for (int x = 0; x < grid.get().width(); x++) {
                if ("t_water_sh".equals(grid.get().get(x, y).getTerrainId())) {
                    sawShallowWater = true;
                }
            }
        }
        assertTrue(sawShallowWater);
    }

    @Test
    void buildsConnectedRoadPatch() throws Exception {
        final OvermapConnectionRegistry connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGrid overmap = OvermapGridFactory.empty(3, 3, "test_field");
        overmap.setOmtId(1, 0, "test_road_ns");
        overmap.setOmtId(1, 1, "test_road_ns");
        overmap.setOmtId(1, 2, "test_road_ns");

        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            overmap,
            1,
            1,
            "test_road_ns",
            5L,
            connections,
            null,
            RegionGroundcoverSettings.single("t_grass")
        );
        assertTrue(grid.isPresent());
        assertEquals("t_pavement", grid.get().get(12, 12).getTerrainId());
        assertEquals("t_grass", grid.get().get(0, 0).getTerrainId());
    }
}
