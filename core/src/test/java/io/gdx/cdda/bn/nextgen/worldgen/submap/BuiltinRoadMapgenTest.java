package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionGroundcoverSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionMapExtrasSettings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinRoadMapgenTest {

    private OvermapTerrainRegistry registry;
    private OvermapConnectionRegistry connections;

    @BeforeEach
    void load() throws Exception {
        registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
    }

    @Test
    void teePaintsNorthEastSouthArms() {
        final OvermapGrid overmap = new OvermapGrid(3, 3, "test_field");
        overmap.setOmtId(1, 0, "test_road_ns");
        overmap.setOmtId(1, 1, "test_road_nes");
        overmap.setOmtId(1, 2, "test_road_ns");
        overmap.setOmtId(2, 1, "test_road_ew");

        final MapGrid grid = BuiltinRoadMapgen.generate(
            overmap, 1, 1, "test_road_nes", connections, registry,
            RegionGroundcoverSettings.defaults(), 42L, false, null
        );

        assertTrue(isPavement(grid, 12, 0), "north arm");
        assertTrue(isPavement(grid, 12, 23), "south arm");
        assertTrue(isPavement(grid, 23, 12), "east arm");
        assertFalse(isPavement(grid, 0, 12), "no west arm");
    }

    @Test
    void sidewalkWhenNeighborHasFlag() {
        final OvermapGrid overmap = new OvermapGrid(3, 3, "test_field");
        overmap.setOmtId(1, 1, "test_road_ns");
        overmap.setOmtId(1, 0, "test_road_ns");
        overmap.setOmtId(1, 2, "test_road_ns");
        overmap.setOmtId(0, 1, "test_building_sidewalk");

        final MapGrid grid = BuiltinRoadMapgen.generate(
            overmap, 1, 1, "test_road_ns", connections, registry,
            RegionGroundcoverSettings.defaults(), 7L, false, null
        );

        boolean foundSidewalk = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if ("t_sidewalk".equals(grid.get(x, y).getTerrainId())) {
                    foundSidewalk = true;
                }
            }
        }
        assertTrue(foundSidewalk, "expected sidewalk strip near urban neighbor");
        // Carriageway must stay pavement — sidewalks must not overwrite the road.
        assertTrue(isPavement(grid, 12, 0), "north carriageway");
        assertTrue(isPavement(grid, 12, 12), "center carriageway");
    }

    @Test
    void sidewalkBesideShop() {
        final OvermapGrid overmap = new OvermapGrid(3, 3, "test_field");
        overmap.setOmtId(1, 1, "test_road_ns");
        overmap.setOmtId(1, 0, "test_road_ns");
        overmap.setOmtId(1, 2, "test_road_ns");
        overmap.setOmtId(0, 1, "test_shop");

        final MapGrid grid = BuiltinRoadMapgen.generate(
            overmap, 1, 1, "test_road_ns", connections, registry,
            RegionGroundcoverSettings.defaults(), 11L, false, null
        );

        boolean foundSidewalk = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if ("t_sidewalk".equals(grid.get(x, y).getTerrainId())) {
                    foundSidewalk = true;
                }
            }
        }
        assertTrue(foundSidewalk, "shop neighbor should trigger sidewalk");
    }

    @Test
    void sidewalkOnEastWestRoadSouthOfHouse() {
        // EW roads use rot=1; sidewalk 8-array must use BN's 45° shuffle, not a flat shift.
        final OvermapGrid overmap = new OvermapGrid(3, 3, "test_field");
        overmap.setOmtId(0, 1, "test_road_ew");
        overmap.setOmtId(1, 1, "test_road_ew");
        overmap.setOmtId(2, 1, "test_road_ew");
        overmap.setOmtId(1, 2, "test_urban_house");

        final MapGrid grid = BuiltinRoadMapgen.generate(
            overmap, 1, 1, "test_road_ew", connections, registry,
            RegionGroundcoverSettings.defaults(), 13L, false, null
        );

        // South of EW pavement should be sidewalk (house is south); north may be grass.
        boolean southSidewalk = false;
        for (int x = 0; x < grid.width(); x++) {
            if ("t_sidewalk".equals(grid.get(x, 20).getTerrainId())
                || "t_sidewalk".equals(grid.get(x, 22).getTerrainId())) {
                southSidewalk = true;
                break;
            }
        }
        assertTrue(southSidewalk, "EW road with southern house should pave south sidewalk strip");
        assertTrue(isPavement(grid, 0, 12), "west carriageway");
        assertTrue(isPavement(grid, 23, 12), "east carriageway");
    }

    @Test
    void diagonalCornerPaintsContinuousBendNotOrthogonalStub() {
        final OvermapGrid overmap = new OvermapGrid(3, 3, "test_field");
        overmap.setOmtId(1, 0, "test_road_ns");
        overmap.setOmtId(1, 1, "test_road_ne");
        overmap.setOmtId(2, 1, "test_road_ew");

        final MapGrid grid = BuiltinRoadMapgen.generate(
            overmap, 1, 1, "test_road_ne", connections, registry,
            RegionGroundcoverSettings.defaults(), 3L, false, null
        );

        assertTrue(isPavement(grid, 12, 0), "north approach");
        assertTrue(isPavement(grid, 23, 12), "east approach");
        // Orthogonal stub would pave the south arm; NE diagonal must not.
        assertFalse(isPavement(grid, 12, 23), "no south stub from orthogonal fallback");
        assertFalse(isPavement(grid, 0, 12), "no west stub");
    }

    @Test
    void liteContentAndExtraStubPlaceFurniture() {
        final OvermapGrid overmap = new OvermapGrid(1, 1, "test_field");
        overmap.setOmtId(0, 0, "test_road_nesw");
        final MapGrid grid = BuiltinRoadMapgen.generate(
            overmap, 0, 0, "test_road_nesw", connections, registry,
            RegionGroundcoverSettings.defaults(), 99L, true, "mx_roadworks"
        );
        boolean furniture = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (grid.get(x, y).getFurnitureId() != null) {
                    furniture = true;
                }
            }
        }
        assertTrue(furniture, "expected litter or roadworks furniture");
    }

    @Test
    void mapExtrasRollCanReturnId() {
        String found = null;
        for (long seed = 0; seed < 5000 && found == null; seed++) {
            found = RoadMapExtras.roll(seed, new RegionMapExtrasSettings(1, RegionMapExtrasSettings.roadDefaults().getWeights()));
        }
        assertNotNull(found);
        assertTrue(found.startsWith("mx_"));
    }

    private static boolean isPavement(final MapGrid grid, final int x, final int y) {
        final String ter = grid.get(x, y).getTerrainId();
        return "t_pavement".equals(ter) || "t_pavement_y".equals(ter);
    }
}
