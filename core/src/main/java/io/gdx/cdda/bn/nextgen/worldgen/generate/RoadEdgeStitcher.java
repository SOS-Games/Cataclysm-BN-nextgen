package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

/**
 * Copies road OMTs across overmap edges from west/north neighbors (R5 neighbor road stitch).
 */
final class RoadEdgeStitcher {

    private RoadEdgeStitcher() {}

    static int stitch(
        final OvermapGrid grid,
        final OvermapNeighborContext neighbors,
        final OvermapConnectionRegistry connections
    ) {
        if (grid == null) {
            return 0;
        }
        final OvermapNeighborContext ctx = neighbors == null ? OvermapNeighborContext.empty() : neighbors;
        int painted = 0;
        if (ctx.getNorth() != null) {
            painted += stitchHorizontal(grid, ctx.getNorth(), true, connections);
        }
        if (ctx.getWest() != null) {
            painted += stitchVertical(grid, ctx.getWest(), true, connections);
        }
        if (ctx.getSouth() != null) {
            painted += stitchHorizontal(grid, ctx.getSouth(), false, connections);
        }
        if (ctx.getEast() != null) {
            painted += stitchVertical(grid, ctx.getEast(), false, connections);
        }
        return painted;
    }

    private static int stitchHorizontal(
        final OvermapGrid grid,
        final OvermapGrid neighbor,
        final boolean fromNorth,
        final OvermapConnectionRegistry connections
    ) {
        final int ny = fromNorth ? neighbor.height() - 1 : 0;
        final int gy = fromNorth ? 0 : grid.height() - 1;
        final int width = Math.min(grid.width(), neighbor.width());
        int painted = 0;
        for (int x = 0; x < width; x++) {
            final String neighborId = neighbor.getOmtId(x, ny);
            if (!RoadConnectionPolisher.isRoadFamily(neighborId, connections)) {
                continue;
            }
            final String existing = grid.getOmtId(x, gy);
            if (RoadConnectionPolisher.isRoadFamily(existing, connections)) {
                continue;
            }
            grid.setOmtId(x, gy, neighborId);
            painted++;
        }
        return painted;
    }

    private static int stitchVertical(
        final OvermapGrid grid,
        final OvermapGrid neighbor,
        final boolean fromWest,
        final OvermapConnectionRegistry connections
    ) {
        final int nx = fromWest ? neighbor.width() - 1 : 0;
        final int gx = fromWest ? 0 : grid.width() - 1;
        final int height = Math.min(grid.height(), neighbor.height());
        int painted = 0;
        for (int y = 0; y < height; y++) {
            final String neighborId = neighbor.getOmtId(nx, y);
            if (!RoadConnectionPolisher.isRoadFamily(neighborId, connections)) {
                continue;
            }
            final String existing = grid.getOmtId(gx, y);
            if (RoadConnectionPolisher.isRoadFamily(existing, connections)) {
                continue;
            }
            grid.setOmtId(gx, y, neighborId);
            painted++;
        }
        return painted;
    }
}
