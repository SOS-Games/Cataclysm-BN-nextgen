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
