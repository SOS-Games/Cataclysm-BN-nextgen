package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionalTerrainResolver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** BN {@code region_terrain_and_furniture} resolve pass at worldgen visit time. */
public final class VisitRegionalResolver {

    private VisitRegionalResolver() {}

    public static void applyToGrid(
        final MapGrid grid,
        final RegionContext regionContext,
        final String regionId,
        final long previewSeed,
        final List<String> warnings
    ) {
        if (grid == null || regionContext == null || regionContext.isEmpty()) {
            return;
        }
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewRegionId(regionId)
            .withPreviewSeed(previewSeed)
            .withRegionContext(regionContext);
        RegionalTerrainResolver.applyToGrid(grid, regionContext, options);
        mergeWarnings(warnings, options.getWarnings());
    }

    public static boolean hasUnresolvedRegionalIds(final MapGrid grid) {
        if (grid == null) {
            return false;
        }
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String terrainId = grid.get(x, y).getTerrainId();
                if (terrainId != null && terrainId.startsWith("t_region_")) {
                    return true;
                }
                final String furnitureId = grid.get(x, y).getFurnitureId();
                if (furnitureId != null && furnitureId.startsWith("f_region_")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void mergeWarnings(final List<String> warnings, final List<String> added) {
        if (warnings == null || added == null || added.isEmpty()) {
            return;
        }
        final Set<String> seen = new HashSet<>(warnings);
        for (final String warning : added) {
            if (seen.add(warning)) {
                warnings.add(warning);
            }
        }
    }
}
