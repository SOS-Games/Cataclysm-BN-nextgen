package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

/**
 * Removes solid double-wide street lanes (full 2×2 road blocks) by alternately clearing
 * the southern and eastern pair until none remain.
 */
public final class ParallelRoadLaneDissolver {

    private ParallelRoadLaneDissolver() {}

    /**
     * @return number of OMTs cleared
     */
    public static int dissolve(final OvermapGrid grid, final String fillId) {
        if (grid == null || fillId == null || fillId.isEmpty()) {
            return 0;
        }
        int cleared = 0;
        for (int pass = 0; pass < 32; pass++) {
            final int south = clearPairs(grid, fillId, true);
            final int east = clearPairs(grid, fillId, false);
            cleared += south + east;
            if (south + east == 0) {
                break;
            }
        }
        return cleared;
    }

    private static int clearPairs(final OvermapGrid grid, final String fillId, final boolean clearSouth) {
        int cleared = 0;
        for (int y = 0; y < grid.height() - 1; y++) {
            for (int x = 0; x < grid.width() - 1; x++) {
                if (!isRoad(grid, x, y)
                    || !isRoad(grid, x + 1, y)
                    || !isRoad(grid, x, y + 1)
                    || !isRoad(grid, x + 1, y + 1)) {
                    continue;
                }
                if (clearSouth) {
                    grid.setOmtId(x, y + 1, fillId);
                    grid.setOmtId(x + 1, y + 1, fillId);
                } else {
                    grid.setOmtId(x + 1, y, fillId);
                    grid.setOmtId(x + 1, y + 1, fillId);
                }
                cleared += 2;
            }
        }
        return cleared;
    }

    private static boolean isRoad(final OvermapGrid grid, final int x, final int y) {
        return UrbanTerrainClearables.isRoadFamily(grid.getOmtId(x, y));
    }
}
