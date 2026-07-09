package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapLakeSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionTerrainNoise;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/** Noise-threshold lake clusters on the overmap (W11b, hydrology v2 shore + river merge). */
public final class LakeGenerator {

    private LakeGenerator() {}

    public static int fill(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || !options.isLakesEnabled() || region == null) {
            return 0;
        }
        final OvermapLakeSettings lakeSettings = region.getLakeSettings();
        if (!lakeSettings.isEnabled()) {
            return 0;
        }
        final String lakeSurfaceId = OrthogonalPathCarver.resolveTerrainId(
            lakeSettings.getLakeSurfaceOterId(),
            options.getLakeId(),
            registry
        );
        final String lakeShoreId = OrthogonalPathCarver.resolveTerrainId(
            lakeSettings.getLakeShoreOterId(),
            lakeSurfaceId,
            registry
        );
        if (registry != null && !registry.contains(lakeSurfaceId)) {
            addWarning(warnings, "lake terrain '" + lakeSurfaceId + "' not in registry; skipping lakes");
            return 0;
        }

        final Set<String> clearable = lakeClearableIds(options, registry, lakeSettings);
        final boolean[][] visited = new boolean[grid.height()][grid.width()];
        int painted = 0;
        final long noiseSeed = options.getSeed() ^ 0x1A4EL;

        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (visited[y][x]) {
                    continue;
                }
                if (!isLakeSeedCell(grid, clearable, noiseSeed, x, y, lakeSettings)) {
                    continue;
                }
                final Set<int[]> cluster = floodLakeCluster(
                    grid,
                    clearable,
                    noiseSeed,
                    x,
                    y,
                    lakeSettings,
                    visited
                );
                if (cluster.size() < lakeSettings.getLakeSizeMin()) {
                    continue;
                }
                final Set<Long> lakeSet = buildLakeSet(grid, cluster, options, registry);
                absorbTouchingRivers(grid, cluster, lakeSet, options, registry);
                painted += paintLakeCluster(
                    grid,
                    cluster,
                    lakeSet,
                    lakeSurfaceId,
                    lakeShoreId,
                    lakeSettings
                );
            }
        }
        return painted;
    }

    private static Set<Long> buildLakeSet(
        final OvermapGrid grid,
        final Set<int[]> cluster,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final Set<Long> lakeSet = new HashSet<>();
        for (final int[] cell : cluster) {
            lakeSet.add(cellKey(cell[0], cell[1]));
        }
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, y), options, registry)) {
                    lakeSet.add(cellKey(x, y));
                }
            }
        }
        return lakeSet;
    }

    private static void absorbTouchingRivers(
        final OvermapGrid grid,
        final Set<int[]> cluster,
        final Set<Long> lakeSet,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, y), options, registry)) {
                    continue;
                }
                if (touchesCluster(x, y, cluster)) {
                    lakeSet.add(cellKey(x, y));
                    cluster.add(new int[] { x, y });
                }
            }
        }
    }

    private static boolean touchesCluster(final int x, final int y, final Set<int[]> cluster) {
        for (final int[] cell : cluster) {
            final int dx = Math.abs(cell[0] - x);
            final int dy = Math.abs(cell[1] - y);
            if (dx + dy == 1) {
                return true;
            }
        }
        return false;
    }

    private static int paintLakeCluster(
        final OvermapGrid grid,
        final Set<int[]> cluster,
        final Set<Long> lakeSet,
        final String lakeSurfaceId,
        final String lakeShoreId,
        final OvermapLakeSettings lakeSettings
    ) {
        int painted = 0;
        final boolean distinctShore = lakeSettings.hasDistinctShoreAndSurface()
            && !lakeSurfaceId.equals(lakeShoreId);
        for (final int[] cell : cluster) {
            final int x = cell[0];
            final int y = cell[1];
            final String paintId;
            if (distinctShore && isShoreCell(x, y, lakeSet)) {
                paintId = lakeShoreId;
            } else {
                paintId = lakeSurfaceId;
            }
            grid.setOmtId(x, y, paintId);
            painted++;
        }
        return painted;
    }

    private static boolean isShoreCell(final int x, final int y, final Set<Long> lakeSet) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (!lakeSet.contains(cellKey(x + dx, y + dy))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long cellKey(final int x, final int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private static Set<int[]> floodLakeCluster(
        final OvermapGrid grid,
        final Set<String> clearable,
        final long noiseSeed,
        final int startX,
        final int startY,
        final OvermapLakeSettings lakeSettings,
        final boolean[][] visited
    ) {
        final Set<int[]> cluster = new HashSet<>();
        final Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { startX, startY });
        while (!queue.isEmpty()) {
            final int[] cell = queue.remove();
            final int x = cell[0];
            final int y = cell[1];
            if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height() || visited[y][x]) {
                continue;
            }
            if (!isLakeSeedCell(grid, clearable, noiseSeed, x, y, lakeSettings)) {
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

    private static boolean isLakeSeedCell(
        final OvermapGrid grid,
        final Set<String> clearable,
        final long noiseSeed,
        final int x,
        final int y,
        final OvermapLakeSettings lakeSettings
    ) {
        if (!clearable.contains(grid.getOmtId(x, y))) {
            return false;
        }
        return RegionTerrainNoise.normalized(noiseSeed, x, y) >= lakeSettings.getNoiseThresholdLake();
    }

    private static Set<String> lakeClearableIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final OvermapLakeSettings lakeSettings
    ) {
        final Set<String> clearable = new HashSet<>(OmtBuildingBlitter.defaultClearableIds(options, registry));
        for (final String terrainId : lakeSettings.getShoreExtendableTerrains()) {
            if (terrainId != null && !terrainId.isEmpty()) {
                clearable.add(terrainId);
            }
        }
        clearable.add(options.getFieldId());
        clearable.add(options.getForestId());
        return clearable;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
