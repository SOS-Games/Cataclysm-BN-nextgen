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

/** Noise-threshold lake clusters on the overmap (W11b). */
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
        final String lakeId = OrthogonalPathCarver.resolveTerrainId(
            lakeSettings.getLakeOterId(),
            options.getLakeId(),
            registry
        );
        if (registry != null && !registry.contains(lakeId)) {
            addWarning(warnings, "lake terrain '" + lakeId + "' not in registry; skipping lakes");
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
                for (final int[] cell : cluster) {
                    grid.setOmtId(cell[0], cell[1], lakeId);
                    painted++;
                }
            }
        }
        return painted;
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
