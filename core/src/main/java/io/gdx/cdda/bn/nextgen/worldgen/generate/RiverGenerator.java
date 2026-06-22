package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Carves river chains on a mini-overmap (W5 v1, W11b lake-aware). */
public final class RiverGenerator {

    private RiverGenerator() {}

    public static int carve(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || !options.isRiversEnabled()) {
            return 0;
        }
        final String riverCenterId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverCenterId(),
            "river_center",
            registry
        );
        final String riverBankId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverBankId(),
            "river",
            registry
        );
        if (registry != null && !registry.contains(riverCenterId)) {
            addWarning(warnings, "river terrain '" + riverCenterId + "' not in registry; skipping river carve");
            return 0;
        }

        final String lakeId = OrthogonalPathCarver.resolveTerrainId(options.getLakeId(), "lake", registry);
        final List<int[]> endpoints = collectRiverEndpoints(grid, lakeId);
        final int startX;
        final int startY;
        final int endX;
        final int endY;
        if (endpoints.size() >= 2) {
            final int[] start = endpoints.get(rng.nextInt(endpoints.size()));
            int[] end = endpoints.get(rng.nextInt(endpoints.size()));
            int guard = 8;
            while ((end[0] == start[0] && end[1] == start[1]) && guard-- > 0) {
                end = endpoints.get(rng.nextInt(endpoints.size()));
            }
            startX = start[0];
            startY = start[1];
            endX = end[0];
            endY = end[1];
        } else {
            startX = rng.nextInt(Math.max(1, grid.width()));
            startY = 0;
            endX = rng.nextInt(Math.max(1, grid.width()));
            endY = grid.height() - 1;
        }

        final List<int[]> path = OrthogonalPathCarver.buildPath(startX, startY, endX, endY, rng);
        if (path.size() < 3) {
            addWarning(warnings, "river path too short");
            return 0;
        }

        final Set<String> overwritable = OrthogonalPathCarver.terrainOverwritableIds(options, registry);
        int painted = OrthogonalPathCarver.paintPath(grid, path, riverCenterId, overwritable);
        if (registry != null && registry.contains(riverBankId)) {
            painted += paintRiverBanks(grid, path, riverBankId, riverCenterId, lakeId, overwritable);
        }
        return painted;
    }

    private static List<int[]> collectRiverEndpoints(final OvermapGrid grid, final String lakeId) {
        final List<int[]> endpoints = new ArrayList<>();
        if (grid == null || lakeId == null || lakeId.isEmpty()) {
            return endpoints;
        }
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!lakeId.equals(grid.getOmtId(x, y))) {
                    continue;
                }
                if (isLakeShore(grid, x, y, lakeId)) {
                    endpoints.add(new int[] { x, y });
                }
            }
        }
        return endpoints;
    }

    private static boolean isLakeShore(final OvermapGrid grid, final int x, final int y, final String lakeId) {
        return isDifferentOrEdge(grid, x + 1, y, lakeId)
            || isDifferentOrEdge(grid, x - 1, y, lakeId)
            || isDifferentOrEdge(grid, x, y + 1, lakeId)
            || isDifferentOrEdge(grid, x, y - 1, lakeId);
    }

    private static boolean isDifferentOrEdge(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String lakeId
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return true;
        }
        return !lakeId.equals(grid.getOmtId(x, y));
    }

    private static int paintRiverBanks(
        final OvermapGrid grid,
        final List<int[]> path,
        final String bankId,
        final String centerId,
        final String lakeId,
        final Set<String> overwritableIds
    ) {
        int painted = 0;
        for (final int[] cell : path) {
            final int x = cell[0];
            final int y = cell[1];
            painted += paintBankAt(grid, x + 1, y, bankId, centerId, lakeId, overwritableIds);
            painted += paintBankAt(grid, x - 1, y, bankId, centerId, lakeId, overwritableIds);
            painted += paintBankAt(grid, x, y + 1, bankId, centerId, lakeId, overwritableIds);
            painted += paintBankAt(grid, x, y - 1, bankId, centerId, lakeId, overwritableIds);
        }
        return painted;
    }

    private static int paintBankAt(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String bankId,
        final String centerId,
        final String lakeId,
        final Set<String> overwritableIds
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return 0;
        }
        final String current = grid.getOmtId(x, y);
        if (centerId.equals(current) || lakeId.equals(current)) {
            return 0;
        }
        if (!overwritableIds.contains(current)) {
            return 0;
        }
        grid.setOmtId(x, y, bankId);
        return 1;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
