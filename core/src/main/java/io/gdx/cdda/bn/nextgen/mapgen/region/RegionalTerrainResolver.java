package io.gdx.cdda.bn.nextgen.mapgen.region;

import io.gdx.cdda.bn.nextgen.map.MapCell;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/** Post-pass regional alias resolve on {@link MapGrid} cells (P11). */
public final class RegionalTerrainResolver {

    private RegionalTerrainResolver() {}

    public static void applyToGrid(
        final MapGrid grid,
        final RegionContext context,
        final JsonMapgenRunOptions options
    ) {
        if (grid == null || context == null || context.isEmpty()) {
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final String regionId = runOptions.getPreviewRegionId();
        final Set<String> warned = new HashSet<>();
        final Consumer<String> warningSink = message -> {
            if (warned.add(message)) {
                runOptions.addWarning(message);
            }
        };

        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final MapCell cell = grid.get(x, y);
                final Random cellRng = cellRng(runOptions, x, y);
                final String terrainId = cell.getTerrainId();
                if (terrainId != null && !terrainId.isEmpty()) {
                    final String resolved = context.resolveTerrain(regionId, terrainId, cellRng, warningSink);
                    if (!terrainId.equals(resolved)) {
                        grid.setTerrain(x, y, resolved);
                    }
                }
                final String furnitureId = cell.getFurnitureId();
                if (furnitureId != null && !furnitureId.isEmpty()) {
                    final String resolved = context.resolveFurniture(regionId, furnitureId, cellRng, warningSink);
                    if (!furnitureId.equals(resolved)) {
                        grid.setFurniture(x, y, resolved);
                    }
                }
            }
        }
    }

    private static Random cellRng(final JsonMapgenRunOptions options, final int x, final int y) {
        final long seed = options.getPreviewSeed() ^ 0xC0DA11L ^ (x * 374761393L) ^ (y * 668265263L);
        return new Random(seed);
    }
}
