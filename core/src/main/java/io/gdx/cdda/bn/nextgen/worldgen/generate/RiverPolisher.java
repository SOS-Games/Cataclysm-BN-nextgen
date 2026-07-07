package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;

/** Removes dangling river spurs and orphan bank tiles (W17d lite). */
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
