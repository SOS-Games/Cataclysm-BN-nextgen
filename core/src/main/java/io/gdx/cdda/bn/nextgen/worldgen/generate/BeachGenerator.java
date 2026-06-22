package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapLakeSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapTerrainSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Paints beach OMTs along lake and river shores (W14c). */
public final class BeachGenerator {

    private BeachGenerator() {}

    public static int paint(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        if (grid == null || options == null || region == null) {
            return 0;
        }
        final OvermapTerrainSettings terrain = region.getTerrainSettings();
        if (!terrain.hasBeachPass()) {
            return 0;
        }
        final String beachId = OrthogonalPathCarver.resolveTerrainId(
            terrain.getBeachOter(),
            "beach",
            registry
        );
        if (registry != null && !registry.contains(beachId)) {
            addWarning(warnings, "beach terrain '" + beachId + "' not in registry; skipping beach pass");
            return 0;
        }
        final Set<String> waterIds = waterIds(options, registry, region);
        final Set<String> shoreClearable = shoreClearableIds(options, registry, region);
        int painted = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!waterIds.contains(grid.getOmtId(x, y))) {
                    continue;
                }
                painted += paintShoreNeighbor(grid, x - 1, y, beachId, shoreClearable);
                painted += paintShoreNeighbor(grid, x + 1, y, beachId, shoreClearable);
                painted += paintShoreNeighbor(grid, x, y - 1, beachId, shoreClearable);
                painted += paintShoreNeighbor(grid, x, y + 1, beachId, shoreClearable);
            }
        }
        return painted;
    }

    private static int paintShoreNeighbor(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String beachId,
        final Set<String> shoreClearable
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return 0;
        }
        final String current = grid.getOmtId(x, y);
        if (!shoreClearable.contains(current)) {
            return 0;
        }
        grid.setOmtId(x, y, beachId);
        return 1;
    }

    private static Set<String> waterIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final RegionSettingsDefinition region
    ) {
        final Set<String> ids = new HashSet<>();
        ids.add(OrthogonalPathCarver.resolveTerrainId(
            region.getLakeSettings().getLakeOterId(),
            options.getLakeId(),
            registry
        ));
        ids.add(OrthogonalPathCarver.resolveTerrainId(
            options.getRiverCenterId(),
            "river_center",
            registry
        ));
        ids.add(OrthogonalPathCarver.resolveTerrainId(
            options.getRiverBankId(),
            "river",
            registry
        ));
        ids.add("test_lake");
        ids.add("test_river");
        return ids;
    }

    private static Set<String> shoreClearableIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final RegionSettingsDefinition region
    ) {
        final Set<String> ids = new HashSet<>(OmtBuildingBlitter.defaultClearableIds(options, registry));
        final OvermapLakeSettings lake = region.getLakeSettings();
        for (final String shore : lake.getShoreExtendableTerrains()) {
            ids.add(shore);
        }
        ids.add(region.getForestSettings().getForestOter());
        ids.add(region.getDefaultOter());
        return ids;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
