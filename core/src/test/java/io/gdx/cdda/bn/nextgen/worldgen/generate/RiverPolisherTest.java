package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiverPolisherTest {

    @Test
    void removesIsolatedRiverCenterTile() {
        final OvermapGrid grid = new OvermapGrid(8, 8, "test_field");
        grid.setOmtId(4, 4, "test_river");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(8, 8)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "local_road", "test_river", "test_river_bank");

        final int smoothed = RiverPolisher.smooth(grid, options, null, new ArrayList<>());

        assertTrue(smoothed >= 1);
        assertEquals("test_field", grid.getOmtId(4, 4));
    }

    @Test
    void removesSingleTileSpurOffJunction() {
        final OvermapGrid grid = new OvermapGrid(8, 8, "test_field");
        grid.setOmtId(1, 1, "test_river");
        grid.setOmtId(2, 1, "test_river");
        grid.setOmtId(3, 1, "test_river");
        grid.setOmtId(2, 2, "test_river");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(8, 8)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "local_road", "test_river", "test_river_bank");

        assertTrue(countRiverNeighbors(grid, 2, 1, "test_river") >= 3);
        final int smoothed = RiverPolisher.smooth(grid, options, null, new ArrayList<>());

        assertTrue(smoothed >= 1, "smoothed=" + smoothed);
        assertEquals("test_field", grid.getOmtId(2, 2));
        assertEquals("test_river", grid.getOmtId(2, 1));
    }

    @Test
    void removesOrphanRiverBank() {
        final OvermapGrid grid = new OvermapGrid(8, 8, "test_field");
        grid.setOmtId(2, 2, "test_river");
        grid.setOmtId(3, 2, "test_river");
        grid.setOmtId(5, 5, "test_river_bank");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(8, 8)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "local_road", "test_river", "test_river_bank");

        final int smoothed = RiverPolisher.smooth(grid, options, null, new ArrayList<>());

        assertTrue(smoothed >= 1);
        assertEquals("test_field", grid.getOmtId(5, 5));
        assertEquals("test_river", grid.getOmtId(2, 2));
        assertEquals("test_river", grid.getOmtId(3, 2));
    }

    private static int countRiverNeighbors(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String riverId
    ) {
        int count = 0;
        for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            final int nx = x + step[0];
            final int ny = y + step[1];
            if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                continue;
            }
            if (riverId.equals(grid.getOmtId(nx, ny))) {
                count++;
            }
        }
        return count;
    }
}
