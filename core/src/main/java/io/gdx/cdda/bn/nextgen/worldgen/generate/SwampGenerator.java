package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapTerrainSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionTerrainNoise;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Swamp clusters from region noise thresholds (W14c). */
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
            "swamp",
            registry
        );
        if (registry != null && !registry.contains(swampId)) {
            addWarning(warnings, "swamp terrain '" + swampId + "' not in registry; skipping swamp pass");
            return 0;
        }
        final Set<String> waterIds = waterIds(options, registry, region);
        final Set<String> clearable = swampClearableIds(options, registry, region);
        final long noiseSeed = options.getSeed() ^ 0x57414D50L;
        int painted = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String current = grid.getOmtId(x, y);
                if (!clearable.contains(current)) {
                    continue;
                }
                final double threshold = isAdjacentToWater(grid, x, y, waterIds)
                    ? terrain.getNoiseThresholdSwampAdjacentWater()
                    : terrain.getNoiseThresholdSwampIsolated();
                if (threshold <= 0.0) {
                    continue;
                }
                final double noise = RegionTerrainNoise.normalized(noiseSeed, x, y);
                if (noise < threshold) {
                    grid.setOmtId(x, y, swampId);
                    painted++;
                }
            }
        }
        return painted;
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

    private static Set<String> swampClearableIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final RegionSettingsDefinition region
    ) {
        final Set<String> ids = new HashSet<>(OmtBuildingBlitter.defaultClearableIds(options, registry));
        ids.add(region.getForestSettings().getForestOter());
        ids.add(region.getDefaultOter());
        return ids;
    }

    private static boolean isAdjacentToWater(
        final OvermapGrid grid,
        final int x,
        final int y,
        final Set<String> waterIds
    ) {
        return isWater(grid, x - 1, y, waterIds)
            || isWater(grid, x + 1, y, waterIds)
            || isWater(grid, x, y - 1, waterIds)
            || isWater(grid, x, y + 1, waterIds);
    }

    private static boolean isWater(
        final OvermapGrid grid,
        final int x,
        final int y,
        final Set<String> waterIds
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return waterIds.contains(grid.getOmtId(x, y));
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
