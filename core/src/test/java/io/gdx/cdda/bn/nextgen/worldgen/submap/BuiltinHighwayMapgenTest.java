package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionGroundcoverSettings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinHighwayMapgenTest {

    @Test
    void northSouthHasSideRailingsAndCenterYellow() {
        final MapGrid grid = BuiltinHighwayMapgen.generate(
            "hiway_ns",
            RegionGroundcoverSettings.defaults(),
            1L
        );
        assertEquals("t_railing", grid.get(3, 12).getTerrainId());
        assertEquals("t_railing", grid.get(20, 12).getTerrainId());
        assertTrue(
            "t_pavement_y".equals(grid.get(11, 1).getTerrainId())
                || "t_pavement_y".equals(grid.get(12, 1).getTerrainId()),
            "expected yellow dash near center"
        );
        assertEquals("t_pavement", grid.get(8, 8).getTerrainId());
        // shoulders stay groundcover
        assertTrue(!"t_pavement".equals(grid.get(1, 8).getTerrainId()));
    }

    @Test
    void eastWestRotatesStrip() {
        final MapGrid grid = BuiltinHighwayMapgen.generate(
            "hiway_ew",
            RegionGroundcoverSettings.defaults(),
            2L
        );
        // After 90° CW: (3,12)→(11,3), (20,12)→(11,20) for 24×24.
        assertEquals("t_railing", grid.get(11, 3).getTerrainId());
        assertEquals("t_railing", grid.get(11, 20).getTerrainId());
        assertEquals("t_pavement", grid.get(8, 8).getTerrainId());
    }

    @Test
    void detectsHighwayIds() {
        assertTrue(BuiltinHighwayMapgen.isHighwayOmt("hiway_ns"));
        assertTrue(BuiltinHighwayMapgen.isHighwayOmt("test_hiway_ew"));
        assertTrue(!BuiltinHighwayMapgen.isHighwayOmt("test_road_ns"));
    }
}
