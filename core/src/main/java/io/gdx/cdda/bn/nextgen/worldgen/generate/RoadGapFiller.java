package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OmLines;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import java.util.Locale;

/**
 * Paves natural holes that sit in the mouth of multiple road stubs (city-edge “field
 * islands” ringed by unconnected {@code road_end_*} OMTs), without creating solid 2×2
 * double-lane blocks.
 */
public final class RoadGapFiller {

    private RoadGapFiller() {}

    /**
     * @return number of holes paved
     */
    public static int fill(
        final OvermapGrid grid,
        final String roadId,
        final OvermapGenerateOptions options
    ) {
        if (grid == null || roadId == null || roadId.isEmpty()) {
            return 0;
        }
        int filled = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int y = 1; y < grid.height() - 1; y++) {
                for (int x = 1; x < grid.width() - 1; x++) {
                    final String id = grid.getOmtId(x, y);
                    if (!isNaturalHole(id, options)) {
                        continue;
                    }
                    if (!shouldPave(grid, x, y)) {
                        continue;
                    }
                    if (wouldCreateSolidTwoByTwo(grid, x, y)) {
                        continue;
                    }
                    grid.setOmtId(x, y, roadId);
                    filled++;
                    changed = true;
                }
            }
        }
        return filled;
    }

    private static boolean shouldPave(final OvermapGrid grid, final int x, final int y) {
        final boolean n = isRoad(grid, x, y - 1);
        final boolean e = isRoad(grid, x + 1, y);
        final boolean s = isRoad(grid, x, y + 1);
        final boolean w = isRoad(grid, x - 1, y);
        final int count = (n ? 1 : 0) + (e ? 1 : 0) + (s ? 1 : 0) + (w ? 1 : 0);
        if (count >= 3) {
            return true;
        }
        if (n && s && !e && !w && isDeadEnd(grid.getOmtId(x, y - 1)) && isDeadEnd(grid.getOmtId(x, y + 1))) {
            return true;
        }
        if (e && w && !n && !s && isDeadEnd(grid.getOmtId(x + 1, y)) && isDeadEnd(grid.getOmtId(x - 1, y))) {
            return true;
        }
        return false;
    }

    /** True if paving (x,y) would complete a solid 2×2 of roads. */
    private static boolean wouldCreateSolidTwoByTwo(final OvermapGrid grid, final int x, final int y) {
        return isTwoByTwoWithHypothetical(grid, x, y, x - 1, y - 1)
            || isTwoByTwoWithHypothetical(grid, x, y, x, y - 1)
            || isTwoByTwoWithHypothetical(grid, x, y, x - 1, y)
            || isTwoByTwoWithHypothetical(grid, x, y, x, y);
    }

    private static boolean isTwoByTwoWithHypothetical(
        final OvermapGrid grid,
        final int paveX,
        final int paveY,
        final int topLeftX,
        final int topLeftY
    ) {
        if (topLeftX < 0 || topLeftY < 0
            || topLeftX + 1 >= grid.width() || topLeftY + 1 >= grid.height()) {
            return false;
        }
        return isRoadOrPave(grid, topLeftX, topLeftY, paveX, paveY)
            && isRoadOrPave(grid, topLeftX + 1, topLeftY, paveX, paveY)
            && isRoadOrPave(grid, topLeftX, topLeftY + 1, paveX, paveY)
            && isRoadOrPave(grid, topLeftX + 1, topLeftY + 1, paveX, paveY);
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

    private static boolean isDeadEnd(final String omtId) {
        if (omtId == null) {
            return false;
        }
        final String base = stripRoadBase(omtId);
        final int line = OmLines.bitsFromId(omtId, base);
        if (line <= 0) {
            final String lower = omtId.toLowerCase(Locale.ROOT);
            return lower.contains("_end_") || lower.endsWith("_isolated");
        }
        return line == OmLines.NORTH
            || line == OmLines.EAST
            || line == OmLines.SOUTH
            || line == OmLines.WEST;
    }

    private static String stripRoadBase(final String omtId) {
        final String lower = omtId.toLowerCase(Locale.ROOT);
        if (lower.startsWith("test_road")) {
            return "test_road";
        }
        if (lower.startsWith("road")) {
            return "road";
        }
        return omtId;
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
