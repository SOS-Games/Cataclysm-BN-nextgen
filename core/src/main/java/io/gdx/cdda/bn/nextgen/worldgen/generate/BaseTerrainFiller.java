package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.Random;

/** Simple field/forest base fill for mini-overmaps (W4 v1). */
public final class BaseTerrainFiller {

    private BaseTerrainFiller() {}

    public static void fill(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        final String fieldId = pickId(options.getFieldId(), "field", registry);
        final String forestId = pickId(options.getForestId(), "forest", registry);
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
        final OvermapTerrainRegistry registry
    ) {
        if (preferred != null && !preferred.isEmpty()
            && (registry == null || registry.contains(preferred))) {
            return preferred;
        }
        if (registry != null && registry.contains(fallback)) {
            return fallback;
        }
        return preferred == null || preferred.isEmpty() ? fallback : preferred;
    }
}
