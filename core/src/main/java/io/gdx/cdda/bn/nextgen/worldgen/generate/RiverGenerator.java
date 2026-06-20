package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** Carves a simplified river chain on a mini-overmap (W5 v1). */
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
        final String riverId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverCenterId(),
            "river_center",
            registry
        );
        if (registry != null && !registry.contains(riverId)) {
            addWarning(warnings, "river terrain '" + riverId + "' not in registry; skipping river carve");
            return 0;
        }

        final int startX = rng.nextInt(Math.max(1, grid.width()));
        final int endX = rng.nextInt(Math.max(1, grid.width()));
        final List<int[]> path = OrthogonalPathCarver.buildPath(startX, 0, endX, grid.height() - 1, rng);
        if (path.size() < 3) {
            addWarning(warnings, "river path too short");
            return 0;
        }

        final Set<String> overwritable = OrthogonalPathCarver.terrainOverwritableIds(options, registry);
        return OrthogonalPathCarver.paintPath(grid, path, riverId, overwritable);
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
