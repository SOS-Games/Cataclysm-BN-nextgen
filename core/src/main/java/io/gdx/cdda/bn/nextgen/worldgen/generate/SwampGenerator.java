package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapForestSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapTerrainSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionTerrainNoise;

import java.util.List;
import java.util.Random;

/** River floodplain buffer + floodplain noise swamps (BN {@code place_swamps}, W14c / Phase C). */
public final class SwampGenerator {

    private SwampGenerator() {}

    public static int fill(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || region == null) {
            return 0;
        }
        final OvermapTerrainSettings terrain = region.getTerrainSettings();
        if (!terrain.hasSwampPass()) {
            return 0;
        }
        final String swampId = OrthogonalPathCarver.resolveTerrainId(
            terrain.getSwampOter(),
            "forest_water",
            registry
        );
        if (registry != null && !registry.contains(swampId)) {
            addWarning(warnings, "swamp terrain '" + swampId + "' not in registry; skipping swamp pass");
            return 0;
        }

        final OvermapForestSettings forest = region.getForestSettings();
        final int[][] floodplain = buildFloodplain(grid, options, registry, forest, rng);
        final long noiseSeed = options.getSeed();
        final double adjacentThreshold = terrain.getNoiseThresholdSwampAdjacentWater();
        final double isolatedThreshold = terrain.getNoiseThresholdSwampIsolated();

        int painted = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!HydrologyTerrainClassifier.isForestOmt(grid.getOmtId(x, y))) {
                    continue;
                }
                final double noise = RegionTerrainNoise.floodplainNormalized(noiseSeed, x, y);
                final int counter = floodplain[x][y];
                final boolean shouldFlood = counter > 0
                    && !oneIn(rng, counter)
                    && noise > adjacentThreshold;
                final boolean shouldIsolated = noise > isolatedThreshold;
                if (shouldFlood || shouldIsolated) {
                    grid.setOmtId(x, y, swampId);
                    painted++;
                }
            }
        }
        return painted;
    }

    private static int[][] buildFloodplain(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final OvermapForestSettings forest,
        final Random rng
    ) {
        final int width = grid.width();
        final int height = grid.height();
        final int[][] floodplain = new int[width][height];
        final int minDist = forest.getRiverFloodplainBufferDistanceMin();
        final int maxDist = forest.getRiverFloodplainBufferDistanceMax();
        final int span = maxDist - minDist + 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!HydrologyTerrainClassifier.isRiverOmt(grid.getOmtId(x, y), options, registry)) {
                    continue;
                }
                final int radius = minDist + (span <= 1 ? 0 : rng.nextInt(span));
                for (final int[] point : ClosestPointsFirst.spiral(x, y, radius)) {
                    final int px = point[0];
                    final int py = point[1];
                    if (px < 0 || py < 0 || px >= width || py >= height) {
                        continue;
                    }
                    floodplain[px][py]++;
                }
            }
        }
        return floodplain;
    }

    private static boolean oneIn(final Random rng, final int chance) {
        if (chance <= 1) {
            return true;
        }
        return rng.nextInt(chance) == 0;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
