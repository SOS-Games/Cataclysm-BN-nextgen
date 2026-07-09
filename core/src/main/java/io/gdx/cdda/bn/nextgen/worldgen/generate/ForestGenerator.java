package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapForestSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionTerrainNoise;

import java.util.List;

/** BN {@code place_forests}: default-oter-only forest/thick conversion (Phase C slice 2). */
public final class ForestGenerator {

    private ForestGenerator() {}

    public static int placeForests(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        if (grid == null || options == null || region == null) {
            return 0;
        }
        final OvermapForestSettings forest = region.getForestSettings();
        if (forest.getNoiseThresholdForest() <= 0.0) {
            return 0;
        }

        final String defaultOterId = resolveDefaultOter(region, options, registry);
        final String forestId = OrthogonalPathCarver.resolveTerrainId(
            forest.getForestOter(),
            options.getForestId(),
            registry
        );
        final String thickId = OrthogonalPathCarver.resolveTerrainId(
            forest.getForestThickOter(),
            forestId,
            registry
        );
        final boolean thickEnabled = forest.hasThickForest()
            && registry != null
            && registry.contains(thickId)
            && !thickId.equals(forestId);
        if (registry != null && !registry.contains(forestId)) {
            addWarning(warnings, "forest terrain '" + forestId + "' not in registry; skipping forest pass");
            return 0;
        }

        final long noiseSeed = options.getSeed();
        final double forestThreshold = forest.getNoiseThresholdForest();
        final double thickThreshold = forest.getNoiseThresholdForestThick();
        int painted = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!defaultOterId.equals(grid.getOmtId(x, y))) {
                    continue;
                }
                final double noise = RegionTerrainNoise.forestNormalized(noiseSeed, x, y);
                if (thickEnabled && noise > thickThreshold) {
                    grid.setOmtId(x, y, thickId);
                    painted++;
                } else if (noise > forestThreshold) {
                    grid.setOmtId(x, y, forestId);
                    painted++;
                }
            }
        }
        return painted;
    }

    private static String resolveDefaultOter(
        final RegionSettingsDefinition region,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final String preferred = region.getDefaultOter();
        final String fallback = options.getFieldId();
        if (preferred != null && !preferred.isEmpty()
            && (registry == null || registry.contains(preferred))) {
            return preferred;
        }
        if (fallback != null && !fallback.isEmpty()
            && (registry == null || registry.contains(fallback))) {
            return fallback;
        }
        if (registry != null && registry.contains("open_air")) {
            return "open_air";
        }
        return preferred == null || preferred.isEmpty() ? fallback : preferred;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
