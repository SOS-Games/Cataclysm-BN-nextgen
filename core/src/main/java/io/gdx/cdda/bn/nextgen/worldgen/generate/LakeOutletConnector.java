package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;

import java.util.Random;
import java.util.Set;

/** Connects lake north/south extremities to the nearest river OMT (BN {@code place_lakes}). */
final class LakeOutletConnector {

    private LakeOutletConnector() {}

    static int connectAll(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        if (grid == null || options == null || region == null || !options.isLakesEnabled()) {
            return 0;
        }
        final String lakeSurfaceId = OrthogonalPathCarver.resolveTerrainId(
            region.getLakeSettings().getLakeSurfaceOterId(),
            options.getLakeId(),
            registry
        );
        final String lakeShoreId = OrthogonalPathCarver.resolveTerrainId(
            region.getLakeSettings().getLakeShoreOterId(),
            lakeSurfaceId,
            registry
        );
        final boolean[][] visited = new boolean[grid.height()][grid.width()];
        int painted = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (visited[y][x]) {
                    continue;
                }
                final String omtId = grid.getOmtId(x, y);
                if (!isLakeCell(omtId, lakeSurfaceId, lakeShoreId, options, registry)) {
                    continue;
                }
                final Set<int[]> cluster = floodLakeCells(
                    grid,
                    x,
                    y,
                    lakeSurfaceId,
                    lakeShoreId,
                    options,
                    registry,
                    visited
                );
                if (cluster.isEmpty()) {
                    continue;
                }
                painted += connectExtremities(grid, cluster, options, region, registry, rng);
            }
        }
        return painted;
    }

    static int connectExtremities(
        final OvermapGrid grid,
        final Set<int[]> lakeCluster,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        if (grid == null || lakeCluster == null || lakeCluster.isEmpty()) {
            return 0;
        }
        final double riverScale = region == null ? 4.0 : region.getRiverScale();
        if (riverScale <= 0.0) {
            return 0;
        }
        final String riverCenterId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverCenterId(),
            "river_center",
            registry
        );
        final String lakeId = OrthogonalPathCarver.resolveTerrainId(
            region.getLakeSettings().getLakeSurfaceOterId(),
            options.getLakeId(),
            registry
        );
        final Set<String> overwritable = OrthogonalPathCarver.terrainOverwritableIds(options, registry);

        int northmostY = Integer.MAX_VALUE;
        int southmostY = Integer.MIN_VALUE;
        int northmostX = 0;
        int southmostX = 0;
        for (final int[] cell : lakeCluster) {
            if (cell[1] < northmostY) {
                northmostY = cell[1];
                northmostX = cell[0];
            }
            if (cell[1] > southmostY) {
                southmostY = cell[1];
                southmostX = cell[0];
            }
        }

        int painted = 0;
        painted += connectPoint(
            grid,
            northmostX,
            northmostY,
            riverCenterId,
            lakeId,
            riverScale,
            overwritable,
            options,
            registry,
            rng
        );
        if (southmostY != northmostY || southmostX != northmostX) {
            painted += connectPoint(
                grid,
                southmostX,
                southmostY,
                riverCenterId,
                lakeId,
                riverScale,
                overwritable,
                options,
                registry,
                rng
            );
        }
        return painted;
    }

    private static int connectPoint(
        final OvermapGrid grid,
        final int lakeX,
        final int lakeY,
        final String riverCenterId,
        final String lakeId,
        final double riverScale,
        final Set<String> overwritable,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        if (lakeX < 0 || lakeY < 0 || lakeX >= grid.width() || lakeY >= grid.height()) {
            return 0;
        }
        final int[] nearest = findNearestRiver(grid, lakeX, lakeY, options, registry);
        if (nearest == null) {
            return 0;
        }
        final int distance = Math.abs(nearest[0] - lakeX) + Math.abs(nearest[1] - lakeY);
        if (distance <= 0) {
            return 0;
        }
        return RiverDrunkardCarver.carve(
            grid,
            nearest[0],
            nearest[1],
            lakeX,
            lakeY,
            riverCenterId,
            lakeId,
            riverScale,
            overwritable,
            rng
        );
    }

    private static int[] findNearestRiver(
        final OvermapGrid grid,
        final int lakeX,
        final int lakeY,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        int bestDistance = Integer.MAX_VALUE;
        int[] best = null;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, y), options, registry)) {
                    continue;
                }
                final int distance = Math.abs(x - lakeX) + Math.abs(y - lakeY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new int[] { x, y };
                }
            }
        }
        return best;
    }

    private static boolean isLakeCell(
        final String omtId,
        final String lakeSurfaceId,
        final String lakeShoreId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (lakeSurfaceId.equals(omtId) || lakeShoreId.equals(omtId)) {
            return true;
        }
        return HydrologyTerrainClassifier.isLakeOmt(omtId, options, registry);
    }

    private static Set<int[]> floodLakeCells(
        final OvermapGrid grid,
        final int startX,
        final int startY,
        final String lakeSurfaceId,
        final String lakeShoreId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final boolean[][] visited
    ) {
        final Set<int[]> cluster = new java.util.HashSet<>();
        final java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[] { startX, startY });
        while (!queue.isEmpty()) {
            final int[] cell = queue.remove();
            final int x = cell[0];
            final int y = cell[1];
            if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height() || visited[y][x]) {
                continue;
            }
            if (!isLakeCell(grid.getOmtId(x, y), lakeSurfaceId, lakeShoreId, options, registry)) {
                continue;
            }
            visited[y][x] = true;
            cluster.add(cell);
            queue.add(new int[] { x + 1, y });
            queue.add(new int[] { x - 1, y });
            queue.add(new int[] { x, y + 1 });
            queue.add(new int[] { x, y - 1 });
        }
        return cluster;
    }
}
