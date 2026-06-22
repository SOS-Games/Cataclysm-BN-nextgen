package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapForestSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionTerrainNoise;

import java.util.List;

/** Upgrades forest OMTs to thick forest using a second noise threshold (W14c). */
public final class ThickForestGenerator {

    private ThickForestGenerator() {}

    public static int upgrade(
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
        if (!forest.hasThickForest()) {
            return 0;
        }
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
        if (registry != null && !registry.contains(thickId)) {
            addWarning(warnings, "thick forest terrain '" + thickId + "' not in registry; skipping thick pass");
            return 0;
        }
        if (thickId.equals(forestId)) {
            return 0;
        }
        int upgraded = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!forestId.equals(grid.getOmtId(x, y))) {
                    continue;
                }
                final double noise = RegionTerrainNoise.normalized(options.getSeed(), x, y);
                if (noise < forest.getNoiseThresholdForestThick()) {
                    grid.setOmtId(x, y, thickId);
                    upgraded++;
                }
            }
        }
        return upgraded;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
