package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import java.util.Locale;

/**
 * Bridges short gaps between road tips (highway stubs that stopped short of a city street,
 * or city stubs that never met an approaching highway).
 * <p>
 * Paves a natural-hole cell that has exactly two opposite road neighbors when at least one
 * neighbor is a tip (only one road neighbor). That connects cul-de-sacs without filling
 * building-lot rows between parallel streets (those neighbors are not tips).
 * <p>
 * Also joins diagonal near-misses: tip facing a hole whose diagonal neighbor is already road
 * (e.g. highway at (59,55) and city stub at (58,56) → pave (58,55)).
 */
public final class RoadTipBridger {

    private static final int MAX_GAP = 2;
    private static final int[][] CARDINAL = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

    private RoadTipBridger() {}

    /**
     * @return number of cells paved
     */
    public static int bridge(
        final OvermapGrid grid,
        final String roadId,
        final OvermapGenerateOptions options
    ) {
        if (grid == null || roadId == null || roadId.isEmpty()) {
            return 0;
        }
        int paved = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int y = 1; y < grid.height() - 1; y++) {
                for (int x = 1; x < grid.width() - 1; x++) {
                    if (!isNaturalHole(grid.getOmtId(x, y), options)) {
                        continue;
                    }
                    if (tryBridgeAxis(grid, x, y, roadId, true) || tryBridgeAxis(grid, x, y, roadId, false)) {
                        paved++;
                        changed = true;
                    }
                }
            }
        }
        paved += bridgeLongerGaps(grid, roadId, options);
        paved += bridgeDiagonalNearMisses(grid, roadId, options);
        return paved;
    }

    private static boolean tryBridgeAxis(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String roadId,
        final boolean northSouth
    ) {
        final int ax = northSouth ? x : x - 1;
        final int ay = northSouth ? y - 1 : y;
        final int bx = northSouth ? x : x + 1;
        final int by = northSouth ? y + 1 : y;
        if (!isRoad(grid, ax, ay) || !isRoad(grid, bx, by)) {
            return false;
        }
        final boolean orthoA = northSouth ? isRoad(grid, x - 1, y) : isRoad(grid, x, y - 1);
        final boolean orthoB = northSouth ? isRoad(grid, x + 1, y) : isRoad(grid, x, y + 1);
        if (orthoA || orthoB) {
            return false;
        }
        // At least one neighbor must be a tip facing into this hole (not an EW street end).
        final boolean aFacesHole = northSouth
            ? tipFaces(grid, ax, ay, 0, 1)
            : tipFaces(grid, ax, ay, 1, 0);
        final boolean bFacesHole = northSouth
            ? tipFaces(grid, bx, by, 0, -1)
            : tipFaces(grid, bx, by, -1, 0);
        if (!aFacesHole && !bFacesHole) {
            return false;
        }
        if (wouldCreateSolidTwoByTwo(grid, x, y)) {
            return false;
        }
        grid.setOmtId(x, y, roadId);
        return true;
    }

    /** Tip whose sole road neighbor is opposite {@code faceDx,faceDy} (cul-de-sac faces that way). */
    private static boolean tipFaces(
        final OvermapGrid grid,
        final int x,
        final int y,
        final int faceDx,
        final int faceDy
    ) {
        if (!isRoad(grid, x, y) || countCardinalRoads(grid, x, y) != 1) {
            return false;
        }
        return isRoad(grid, x - faceDx, y - faceDy);
    }

    private static int bridgeLongerGaps(
        final OvermapGrid grid,
        final String roadId,
        final OvermapGenerateOptions options
    ) {
        int paved = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!isRoadTip(grid, x, y)) {
                    continue;
                }
                int roadNeighborDir = -1;
                for (int i = 0; i < 4; i++) {
                    if (isRoad(grid, x + CARDINAL[i][0], y + CARDINAL[i][1])) {
                        roadNeighborDir = i;
                        break;
                    }
                }
                if (roadNeighborDir < 0) {
                    continue;
                }
                final int dx = -CARDINAL[roadNeighborDir][0];
                final int dy = -CARDINAL[roadNeighborDir][1];
                paved += bridgeOutward(grid, x, y, dx, dy, roadId, options);
            }
        }
        return paved;
    }

    private static int bridgeOutward(
        final OvermapGrid grid,
        final int tipX,
        final int tipY,
        final int dx,
        final int dy,
        final String roadId,
        final OvermapGenerateOptions options
    ) {
        for (int gap = 1; gap <= MAX_GAP; gap++) {
            final int tx = tipX + dx * gap;
            final int ty = tipY + dy * gap;
            if (tx < 0 || ty < 0 || tx >= grid.width() || ty >= grid.height()) {
                return 0;
            }
            if (isRoad(grid, tx, ty)) {
                int paved = 0;
                for (int i = 1; i < gap; i++) {
                    final int px = tipX + dx * i;
                    final int py = tipY + dy * i;
                    if (!isNaturalHole(grid.getOmtId(px, py), options)) {
                        return 0;
                    }
                    if (wouldCreateSolidTwoByTwo(grid, px, py)) {
                        return 0;
                    }
                    grid.setOmtId(px, py, roadId);
                    paved++;
                }
                return paved;
            }
            if (!isNaturalHole(grid.getOmtId(tx, ty), options)) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Tip faces a natural hole; a road sits on the diagonal ahead → pave the hole for an L-join.
     */
    private static int bridgeDiagonalNearMisses(
        final OvermapGrid grid,
        final String roadId,
        final OvermapGenerateOptions options
    ) {
        int paved = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!isRoadTip(grid, x, y)) {
                    continue;
                }
                int roadNeighborDir = -1;
                for (int i = 0; i < 4; i++) {
                    if (isRoad(grid, x + CARDINAL[i][0], y + CARDINAL[i][1])) {
                        roadNeighborDir = i;
                        break;
                    }
                }
                if (roadNeighborDir < 0) {
                    continue;
                }
                final int dx = -CARDINAL[roadNeighborDir][0];
                final int dy = -CARDINAL[roadNeighborDir][1];
                final int hx = x + dx;
                final int hy = y + dy;
                if (hx < 0 || hy < 0 || hx >= grid.width() || hy >= grid.height()) {
                    continue;
                }
                if (!isNaturalHole(grid.getOmtId(hx, hy), options)) {
                    continue;
                }
                // When outward is N/S (dx=0), perps are E/W (±1,0). When E/W (dy=0), perps are N/S.
                final int[][] perps = dx == 0
                    ? new int[][] { { 1, 0 }, { -1, 0 } }
                    : new int[][] { { 0, 1 }, { 0, -1 } };
                for (final int[] perp : perps) {
                    if (isRoad(grid, hx + perp[0], hy + perp[1])
                        && !wouldCreateSolidTwoByTwo(grid, hx, hy)) {
                        grid.setOmtId(hx, hy, roadId);
                        paved++;
                        break;
                    }
                }
            }
        }
        return paved;
    }

    private static boolean isRoadTip(final OvermapGrid grid, final int x, final int y) {
        return isRoad(grid, x, y) && countCardinalRoads(grid, x, y) == 1;
    }

    private static int countCardinalRoads(final OvermapGrid grid, final int x, final int y) {
        int n = 0;
        if (isRoad(grid, x, y - 1)) {
            n++;
        }
        if (isRoad(grid, x + 1, y)) {
            n++;
        }
        if (isRoad(grid, x, y + 1)) {
            n++;
        }
        if (isRoad(grid, x - 1, y)) {
            n++;
        }
        return n;
    }

    private static boolean wouldCreateSolidTwoByTwo(final OvermapGrid grid, final int x, final int y) {
        for (int ox = x - 1; ox <= x; ox++) {
            for (int oy = y - 1; oy <= y; oy++) {
                if (ox < 0 || oy < 0 || ox + 1 >= grid.width() || oy + 1 >= grid.height()) {
                    continue;
                }
                if (isRoadOrPave(grid, ox, oy, x, y)
                    && isRoadOrPave(grid, ox + 1, oy, x, y)
                    && isRoadOrPave(grid, ox, oy + 1, x, y)
                    && isRoadOrPave(grid, ox + 1, oy + 1, x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRoadOrPave(
        final OvermapGrid grid,
        final int x,
        final int y,
        final int paveX,
        final int paveY
    ) {
        return (x == paveX && y == paveY) || isRoad(grid, x, y);
    }

    private static boolean isNaturalHole(final String omtId, final OvermapGenerateOptions options) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        if (UrbanTerrainClearables.isRoadFamily(omtId)) {
            return false;
        }
        if (UrbanTerrainClearables.isWaterBody(omtId, options)) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        if (n.startsWith("forest_trail") || n.startsWith("test_forest_trail") || n.startsWith("trailhead")) {
            return false;
        }
        return n.equals("field")
            || n.equals("test_field")
            || n.equals("open_air")
            || n.contains("forest")
            || n.contains("swamp");
    }

    private static boolean isRoad(final OvermapGrid grid, final int x, final int y) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return UrbanTerrainClearables.isRoadFamily(grid.getOmtId(x, y));
    }
}
