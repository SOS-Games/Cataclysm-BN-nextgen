package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** BN {@code place_rivers} edge stitch and start/end endpoint collection (hydrology v2). */
final class RiverEdgeStitcher {

    private RiverEdgeStitcher() {}

    static RiverEndpointPlan stitchAndCollect(
        final OvermapGrid grid,
        final OvermapNeighborContext neighbors,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final String riverCenterId,
        final double riverScale,
        final Random rng
    ) {
        final int riverChance = Math.max(1, (int) (1.0 / Math.max(0.01, riverScale)));
        final int brushScale = Math.max(1, (int) riverScale);
        final int edgeMin = edgeMargin(grid);
        final int edgeMaxX = grid.width() - 1 - edgeMargin(grid);
        final int edgeMaxY = grid.height() - 1 - edgeMargin(grid);

        final List<int[]> starts = new ArrayList<>();
        final List<int[]> ends = new ArrayList<>();
        int painted = 0;

        final OvermapNeighborContext ctx = neighbors == null ? OvermapNeighborContext.empty() : neighbors;

        if (ctx.getNorth() != null) {
            painted += stitchHorizontalEdge(
                grid,
                ctx.getNorth(),
                true,
                edgeMin,
                edgeMaxX,
                riverCenterId,
                options,
                registry,
                riverChance,
                brushScale,
                starts,
                rng
            );
        }
        final int riversFromNorth = starts.size();

        if (ctx.getWest() != null) {
            painted += stitchVerticalEdge(
                grid,
                ctx.getWest(),
                true,
                edgeMin,
                edgeMaxY,
                riverCenterId,
                options,
                registry,
                riverChance,
                brushScale,
                starts,
                riversFromNorth,
                rng
            );
        }

        if (ctx.getSouth() != null) {
            painted += stitchHorizontalEdge(
                grid,
                ctx.getSouth(),
                false,
                edgeMin,
                edgeMaxX,
                riverCenterId,
                options,
                registry,
                riverChance,
                brushScale,
                ends,
                rng
            );
        }
        final int riversToSouth = ends.size();

        if (ctx.getEast() != null) {
            painted += stitchVerticalEdge(
                grid,
                ctx.getEast(),
                false,
                edgeMin,
                edgeMaxY,
                riverCenterId,
                options,
                registry,
                riverChance,
                brushScale,
                ends,
                riversToSouth,
                rng
            );
        }

        balanceSyntheticEndpoints(grid, ctx, starts, ends, edgeMin, edgeMaxX, edgeMaxY, riverChance, rng);

        return new RiverEndpointPlan(starts, ends, painted);
    }

    private static int stitchHorizontalEdge(
        final OvermapGrid grid,
        final OvermapGrid neighbor,
        final boolean northEdge,
        final int edgeMin,
        final int edgeMax,
        final String riverCenterId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final int riverChance,
        final int brushScale,
        final List<int[]> endpoints,
        final Random rng
    ) {
        int painted = 0;
        final int localY = northEdge ? 0 : grid.height() - 1;
        final int neighborY = northEdge ? neighbor.height() - 1 : 0;
        for (int x = edgeMin; x <= edgeMax; x++) {
            if (x < 0 || x >= neighbor.width()) {
                continue;
            }
            final String neighborId = neighbor.getOmtId(x, neighborY);
            if (HydrologyTerrainClassifier.isRiverOmt(neighborId, options, registry)) {
                grid.setOmtId(x, localY, riverCenterId);
                painted++;
            }
            final boolean triple = isRiverAt(neighbor, x, neighborY, options, registry)
                && isRiverAt(neighbor, x + 1, neighborY, options, registry)
                && isRiverAt(neighbor, x - 1, neighborY, options, registry);
            if (!triple) {
                continue;
            }
            if (northEdge) {
                if (rng.nextInt(riverChance) != 0) {
                    continue;
                }
                if (!endpoints.isEmpty()) {
                    final int[] last = endpoints.get(endpoints.size() - 1);
                    if (last[0] >= (x - 6) * brushScale) {
                        continue;
                    }
                }
            } else if (!endpoints.isEmpty()) {
                final int[] last = endpoints.get(endpoints.size() - 1);
                if (last[0] >= x - 6) {
                    continue;
                }
            }
            endpoints.add(new int[] { x, localY });
        }
        return painted;
    }

    private static int stitchVerticalEdge(
        final OvermapGrid grid,
        final OvermapGrid neighbor,
        final boolean westEdge,
        final int edgeMin,
        final int edgeMax,
        final String riverCenterId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final int riverChance,
        final int brushScale,
        final List<int[]> endpoints,
        final int priorCount,
        final Random rng
    ) {
        int painted = 0;
        final int localX = westEdge ? 0 : grid.width() - 1;
        final int neighborX = westEdge ? neighbor.width() - 1 : 0;
        for (int y = edgeMin; y <= edgeMax; y++) {
            if (y < 0 || y >= neighbor.height()) {
                continue;
            }
            final String neighborId = neighbor.getOmtId(neighborX, y);
            if (HydrologyTerrainClassifier.isRiverOmt(neighborId, options, registry)) {
                grid.setOmtId(localX, y, riverCenterId);
                painted++;
            }
            final boolean triple = isRiverAt(neighbor, neighborX, y, options, registry)
                && isRiverAt(neighbor, neighborX, y + 1, options, registry)
                && isRiverAt(neighbor, neighborX, y - 1, options, registry);
            if (!triple) {
                continue;
            }
            if (westEdge) {
                if (rng.nextInt(riverChance) != 0) {
                    continue;
                }
                if (endpoints.size() == priorCount) {
                    endpoints.add(new int[] { localX, y });
                } else if (!endpoints.isEmpty()) {
                    final int[] last = endpoints.get(endpoints.size() - 1);
                    if (last[1] < (y - 6) * brushScale) {
                        endpoints.add(new int[] { localX, y });
                    }
                }
            } else if (endpoints.size() == priorCount || endpoints.isEmpty()) {
                endpoints.add(new int[] { localX, y });
            } else {
                final int[] last = endpoints.get(endpoints.size() - 1);
                if (last[1] < y - 6) {
                    endpoints.add(new int[] { localX, y });
                }
            }
        }
        return painted;
    }

    private static void balanceSyntheticEndpoints(
        final OvermapGrid grid,
        final OvermapNeighborContext neighbors,
        final List<int[]> starts,
        final List<int[]> ends,
        final int edgeMin,
        final int edgeMaxX,
        final int edgeMaxY,
        final int riverChance,
        final Random rng
    ) {
        if (neighbors.getNorth() == null || neighbors.getWest() == null) {
            while (starts.isEmpty() || starts.size() + 1 < ends.size()) {
                final List<int[]> picks = new ArrayList<>();
                if (neighbors.getNorth() == null && rng.nextInt(riverChance) == 0) {
                    picks.add(new int[] { randomRange(rng, edgeMin, edgeMaxX), 0 });
                }
                if (neighbors.getWest() == null && rng.nextInt(riverChance) == 0) {
                    picks.add(new int[] { 0, randomRange(rng, edgeMin, edgeMaxY) });
                }
                if (picks.isEmpty()) {
                    picks.add(new int[] { randomRange(rng, edgeMin, edgeMaxX), 0 });
                }
                starts.add(picks.get(rng.nextInt(picks.size())));
            }
        }
        if (neighbors.getSouth() == null || neighbors.getEast() == null) {
            while (ends.isEmpty() || ends.size() + 1 < starts.size()) {
                final List<int[]> picks = new ArrayList<>();
                if (neighbors.getSouth() == null && rng.nextInt(riverChance) == 0) {
                    picks.add(new int[] { randomRange(rng, edgeMin, edgeMaxX), grid.height() - 1 });
                }
                if (neighbors.getEast() == null && rng.nextInt(riverChance) == 0) {
                    picks.add(new int[] { grid.width() - 1, randomRange(rng, edgeMin, edgeMaxY) });
                }
                if (picks.isEmpty()) {
                    picks.add(new int[] { randomRange(rng, edgeMin, edgeMaxX), grid.height() - 1 });
                }
                ends.add(picks.get(rng.nextInt(picks.size())));
            }
        }
    }

    private static int randomRange(final Random rng, final int min, final int max) {
        if (max <= min) {
            return min;
        }
        return min + rng.nextInt(max - min + 1);
    }

    private static boolean isRiverAt(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, y), options, registry);
    }

    private static int edgeMargin(final OvermapGrid grid) {
        return Math.min(2, Math.max(0, Math.min(grid.width(), grid.height()) / 4 - 1));
    }

    static final class RiverEndpointPlan {
        private final List<int[]> starts;
        private final List<int[]> ends;
        private final int stitchedCells;

        RiverEndpointPlan(final List<int[]> starts, final List<int[]> ends, final int stitchedCells) {
            this.starts = starts;
            this.ends = ends;
            this.stitchedCells = stitchedCells;
        }

        List<int[]> getStarts() {
            return starts;
        }

        List<int[]> getEnds() {
            return ends;
        }

        int getStitchedCells() {
            return stitchedCells;
        }
    }
}
