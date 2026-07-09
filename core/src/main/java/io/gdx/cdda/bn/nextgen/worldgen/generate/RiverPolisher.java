package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;

/** Removes dangling river spurs and rewrites topology to directional river OMT ids (hydrology v2). */
public final class RiverPolisher {

    private RiverPolisher() {}

    public static int smooth(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        if (grid == null || options == null) {
            return 0;
        }
        final String centerId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverCenterId(),
            "river_center",
            registry
        );
        final String bankId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverBankId(),
            "river",
            registry
        );
        final String lakeId = OrthogonalPathCarver.resolveTerrainId(options.getLakeId(), "lake", registry);
        final String revertId = OrthogonalPathCarver.resolveTerrainId(options.getFieldId(), "field", registry);
        if (centerId == null || centerId.isEmpty()) {
            addWarning(warnings, "river center terrain missing; skipping river polish");
            return 0;
        }

        int smoothed = 0;
        smoothed += removeOrphanRiverCenters(grid, centerId, revertId);
        smoothed += removeDanglingSpurs(grid, centerId, revertId);
        if (bankId != null && !bankId.isEmpty() && !bankId.equals(centerId)) {
            smoothed += removeOrphanRiverBanks(grid, centerId, bankId, lakeId, revertId);
        }
        return smoothed;
    }

    /**
     * BN {@code polish_rivers} — rewrite river center tiles to directional {@code river_*} OMT ids.
     * Off-map neighbors are treated as water unless {@link OvermapNeighborContext} supplies an edge grid.
     */
    public static int polishDirectional(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        return polishDirectional(grid, options, registry, warnings, OvermapNeighborContext.empty());
    }

    public static int polishDirectional(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final List<String> warnings,
        final OvermapNeighborContext neighbors
    ) {
        if (grid == null || options == null) {
            return 0;
        }
        final OvermapNeighborContext ctx = neighbors == null ? OvermapNeighborContext.empty() : neighbors;
        final String[][] snapshot = snapshotOmtIds(grid);
        int polished = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String omtId = snapshot[y][x];
                if (!HydrologyTerrainClassifier.isRiverOmt(omtId, options, registry)) {
                    continue;
                }
                final boolean waterNorth = waterAt(
                    snapshot, grid, x, y - 1, ctx, options, registry
                );
                final boolean waterWest = waterAt(
                    snapshot, grid, x - 1, y, ctx, options, registry
                );
                final boolean waterSouth = waterAt(
                    snapshot, grid, x, y + 1, ctx, options, registry
                );
                final boolean waterEast = waterAt(
                    snapshot, grid, x + 1, y, ctx, options, registry
                );
                final String polishedId = resolvePolishedRiverIdBn(
                    snapshot,
                    grid.width(),
                    grid.height(),
                    x,
                    y,
                    waterNorth,
                    waterWest,
                    waterSouth,
                    waterEast,
                    options,
                    registry
                );
                if (polishedId != null && !polishedId.equals(omtId)) {
                    grid.setOmtId(x, y, polishedId);
                    polished++;
                }
            }
        }
        return polished;
    }

    private static String[][] snapshotOmtIds(final OvermapGrid grid) {
        final String[][] snapshot = new String[grid.height()][grid.width()];
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                snapshot[y][x] = grid.getOmtId(x, y);
            }
        }
        return snapshot;
    }

    private static String resolvePolishedRiverIdBn(
        final String[][] snapshot,
        final int width,
        final int height,
        final int x,
        final int y,
        final boolean waterNorth,
        final boolean waterWest,
        final boolean waterSouth,
        final boolean waterEast,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (waterWest) {
            if (waterNorth) {
                if (waterSouth) {
                    if (waterEast) {
                        if (dryDiagonal(snapshot, width, height, x - 1, y - 1, options, registry)) {
                            return pickRiverId("c_not_nw", options, registry);
                        }
                        if (dryDiagonal(snapshot, width, height, x + 1, y - 1, options, registry)) {
                            return pickRiverId("c_not_ne", options, registry);
                        }
                        if (dryDiagonal(snapshot, width, height, x - 1, y + 1, options, registry)) {
                            return pickRiverId("c_not_sw", options, registry);
                        }
                        if (dryDiagonal(snapshot, width, height, x + 1, y + 1, options, registry)) {
                            return pickRiverId("c_not_se", options, registry);
                        }
                        return pickRiverId("center", options, registry);
                    }
                    return pickRiverId("east", options, registry);
                }
                if (waterEast) {
                    return pickRiverId("south", options, registry);
                }
                return pickRiverId("se", options, registry);
            }
            if (waterSouth) {
                if (waterEast) {
                    return pickRiverId("north", options, registry);
                }
                return pickRiverId("ne", options, registry);
            }
            if (waterEast) {
                return pickRiverId("north", options, registry);
            }
            return revertNonRiver(options, registry);
        }
        if (waterNorth) {
            if (waterSouth) {
                if (waterEast) {
                    return pickRiverId("west", options, registry);
                }
                if (waterWest) {
                    return pickRiverId("east", options, registry);
                }
                return pickRiverId("north", options, registry);
            }
            if (waterEast) {
                return pickRiverId("sw", options, registry);
            }
            return revertNonRiver(options, registry);
        }
        if (waterSouth) {
            if (waterEast) {
                return pickRiverId("nw", options, registry);
            }
            return revertNonRiver(options, registry);
        }
        return revertNonRiver(options, registry);
    }

    private static boolean dryDiagonal(
        final String[][] snapshot,
        final int width,
        final int height,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return false;
        }
        return !HydrologyTerrainClassifier.isRiverOrLake(snapshot[y][x], options, registry);
    }

    private static String revertNonRiver(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        return OrthogonalPathCarver.resolveTerrainId(options.getFieldId(), "field", registry);
    }

    private static boolean waterAt(
        final String[][] snapshot,
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapNeighborContext neighbors,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final int width = grid.width();
        final int height = grid.height();
        if (x >= 0 && y >= 0 && x < width && y < height) {
            return HydrologyTerrainClassifier.isRiverOrLake(snapshot[y][x], options, registry);
        }
        final String neighborId = neighborOmtIdAt(neighbors, grid, x, y);
        if (neighborId != null) {
            return HydrologyTerrainClassifier.isRiverOrLake(neighborId, options, registry);
        }
        return true;
    }

    private static String neighborOmtIdAt(
        final OvermapNeighborContext neighbors,
        final OvermapGrid grid,
        final int x,
        final int y
    ) {
        final int width = grid.width();
        final int height = grid.height();
        if (x < 0 && neighbors.getWest() != null) {
            final OvermapGrid west = neighbors.getWest();
            final int wx = west.width() - 1;
            if (y >= 0 && y < west.height()) {
                return west.getOmtId(wx, y);
            }
        }
        if (x >= width && neighbors.getEast() != null) {
            final OvermapGrid east = neighbors.getEast();
            if (y >= 0 && y < east.height()) {
                return east.getOmtId(0, y);
            }
        }
        if (y < 0 && neighbors.getNorth() != null) {
            final OvermapGrid north = neighbors.getNorth();
            final int ny = north.height() - 1;
            if (x >= 0 && x < north.width()) {
                return north.getOmtId(x, ny);
            }
        }
        if (y >= height && neighbors.getSouth() != null) {
            final OvermapGrid south = neighbors.getSouth();
            if (x >= 0 && x < south.width()) {
                return south.getOmtId(x, 0);
            }
        }
        return null;
    }

    private static String pickRiverId(
        final String suffix,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final String[] candidates = {
            "river_" + suffix,
            options.getRiverBankId() + "_" + suffix,
            options.getRiverCenterId() + "_" + suffix,
            options.getRiverBankId(),
            options.getRiverCenterId()
        };
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            if (registry == null || registry.contains(candidate)) {
                return candidate;
            }
        }
        return options.getRiverCenterId();
    }

    private static int removeOrphanRiverCenters(
        final OvermapGrid grid,
        final String centerId,
        final String revertId
    ) {
        int removed = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!centerId.equals(grid.getOmtId(x, y))) {
                    continue;
                }
                if (countAdjacentRiverCenters(grid, x, y, centerId) == 0) {
                    grid.setOmtId(x, y, revertId);
                    removed++;
                }
            }
        }
        return removed;
    }

    private static int removeDanglingSpurs(
        final OvermapGrid grid,
        final String centerId,
        final String revertId
    ) {
        int removed = 0;
        boolean changed = true;
        int guard = grid.width() * grid.height();
        while (changed && guard-- > 0) {
            changed = false;
            for (int y = 0; y < grid.height(); y++) {
                for (int x = 0; x < grid.width(); x++) {
                    if (!centerId.equals(grid.getOmtId(x, y))) {
                        continue;
                    }
                    if (countAdjacentRiverCenters(grid, x, y, centerId) != 1) {
                        continue;
                    }
                    final int[] neighbor = soleRiverNeighbor(grid, x, y, centerId);
                    if (neighbor == null) {
                        continue;
                    }
                    if (countAdjacentRiverCenters(grid, neighbor[0], neighbor[1], centerId) < 3) {
                        continue;
                    }
                    if (isColinearRiverEndpoint(grid, x, y, neighbor[0], neighbor[1], centerId)) {
                        continue;
                    }
                    grid.setOmtId(x, y, revertId);
                    removed++;
                    changed = true;
                }
            }
        }
        return removed;
    }

    private static boolean isColinearRiverEndpoint(
        final OvermapGrid grid,
        final int leafX,
        final int leafY,
        final int junctionX,
        final int junctionY,
        final String centerId
    ) {
        for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            final int nx = junctionX + step[0];
            final int ny = junctionY + step[1];
            if (nx == leafX && ny == leafY) {
                continue;
            }
            if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                continue;
            }
            if (!centerId.equals(grid.getOmtId(nx, ny))) {
                continue;
            }
            if ((leafX == junctionX && junctionX == nx) || (leafY == junctionY && junctionY == ny)) {
                return true;
            }
        }
        return false;
    }

    private static int removeOrphanRiverBanks(
        final OvermapGrid grid,
        final String centerId,
        final String bankId,
        final String lakeId,
        final String revertId
    ) {
        int removed = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!bankId.equals(grid.getOmtId(x, y))) {
                    continue;
                }
                if (touchesRiverCenterOrLake(grid, x, y, centerId, lakeId)) {
                    continue;
                }
                grid.setOmtId(x, y, revertId);
                removed++;
            }
        }
        return removed;
    }

    private static boolean touchesRiverCenterOrLake(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String centerId,
        final String lakeId
    ) {
        for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            final int nx = x + step[0];
            final int ny = y + step[1];
            if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                continue;
            }
            final String id = grid.getOmtId(nx, ny);
            if (centerId.equals(id) || (lakeId != null && lakeId.equals(id))) {
                return true;
            }
        }
        return false;
    }

    private static int[] soleRiverNeighbor(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String centerId
    ) {
        int[] found = null;
        for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            final int nx = x + step[0];
            final int ny = y + step[1];
            if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                continue;
            }
            if (!centerId.equals(grid.getOmtId(nx, ny))) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = new int[] { nx, ny };
        }
        return found;
    }

    private static int countAdjacentRiverCenters(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String centerId
    ) {
        int count = 0;
        for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            final int nx = x + step[0];
            final int ny = y + step[1];
            if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                continue;
            }
            if (centerId.equals(grid.getOmtId(nx, ny))) {
                count++;
            }
        }
        return count;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
