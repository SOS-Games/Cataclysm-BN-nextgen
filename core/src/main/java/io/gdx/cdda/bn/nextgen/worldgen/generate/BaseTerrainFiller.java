package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapForestSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionTerrainNoise;

import java.util.Random;

/** Base overmap fill from region settings or v1 forest/field noise (W4/W9). */
public final class BaseTerrainFiller {

    private BaseTerrainFiller() {}

    public static void fill(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        fill(grid, options, null, registry, rng);
    }

    public static void fill(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        if (region == null) {
            fillLegacy(grid, options, registry, rng);
            return;
        }
        fillFromRegion(grid, options, region, registry);
    }

    private static void fillFromRegion(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry
    ) {
        final OvermapForestSettings forest = region.getForestSettings();
        final String defaultId = pickId(region.getDefaultOter(), options.getFieldId(), "field", registry);
        final String forestId = pickId(forest.getForestOter(), options.getForestId(), "forest", registry);
        final String thickId = pickId(forest.getForestThickOter(), forestId, forestId, registry);
        final boolean thickEnabled = forest.hasThickForest()
            && registry != null
            && registry.contains(thickId)
            && !thickId.equals(forestId);

        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final double noise = RegionTerrainNoise.normalized(options.getSeed(), x, y);
                final String omtId;
                if (noise < forest.getNoiseThresholdForest()) {
                    omtId = forestId;
                } else if (thickEnabled && noise < forest.getNoiseThresholdForestThick()) {
                    omtId = thickId;
                } else {
                    omtId = defaultId;
                }
                grid.setOmtId(x, y, omtId);
            }
        }
    }

    private static void fillLegacy(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        final String fieldId = pickId(options.getFieldId(), "field", "field", registry);
        final String forestId = pickId(options.getForestId(), "forest", "forest", registry);
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final int noise = (x * 73856093) ^ (y * 19349663) ^ (int) options.getSeed();
                final boolean forest = rng != null
                    ? rng.nextInt(100) < 35
                    : (Integer.bitCount(noise) & 1) == 1;
                grid.setOmtId(x, y, forest ? forestId : fieldId);
            }
        }
    }

    private static String pickId(
        final String preferred,
        final String fallback,
        final String secondFallback,
        final OvermapTerrainRegistry registry
    ) {
        if (preferred != null && !preferred.isEmpty()
            && (registry == null || registry.contains(preferred))) {
            return preferred;
        }
        if (fallback != null && !fallback.isEmpty()
            && (registry == null || registry.contains(fallback))) {
            return fallback;
        }
        if (secondFallback != null && !secondFallback.isEmpty()
            && (registry == null || registry.contains(secondFallback))) {
            return secondFallback;
        }
        if (registry != null && registry.contains("open_air")) {
            return "open_air";
        }
        return preferred == null || preferred.isEmpty()
            ? (fallback == null || fallback.isEmpty() ? secondFallback : fallback)
            : preferred;
    }
}
