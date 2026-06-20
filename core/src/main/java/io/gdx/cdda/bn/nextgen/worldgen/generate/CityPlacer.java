package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Places {@code city_building} bundles on an overmap grid (W4). */
public final class CityPlacer {

    private CityPlacer() {}

    public static int placeAll(
        final OvermapGrid grid,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapGenerateOptions options,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters
    ) {
        if (grid == null || buildings == null || options.getCityBuildingQuota() <= 0) {
            return 0;
        }
        final Set<String> clearable = OmtBuildingBlitter.defaultClearableIds(options, oterRegistry);
        final List<CityBuildingDefinition> candidates = pickCandidates(buildings, grid);
        if (candidates.isEmpty()) {
            addWarning(warnings, "no city_building candidates for placement");
            return 0;
        }
        shuffle(candidates, rng);

        int placed = 0;
        int attempts = 0;
        final int maxAttempts = options.getCityBuildingQuota() * candidates.size() * 2;
        while (placed < options.getCityBuildingQuota() && attempts < maxAttempts && !candidates.isEmpty()) {
            attempts++;
            final CityBuildingDefinition building = candidates.get(rng.nextInt(candidates.size()));
            if (tryPlace(building, grid, oterRegistry, clearable, rng, warnings, placedCenters)) {
                placed++;
            }
        }
        return placed;
    }

    public static boolean tryPlace(
        final CityBuildingDefinition building,
        final OvermapGrid grid,
        final OvermapTerrainRegistry oterRegistry,
        final Set<String> clearableIds,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters
    ) {
        if (building == null) {
            return false;
        }
        final BuildingFootprint footprint = BuildingFootprint.atZ(building, 0);
        if (footprint.isEmpty()) {
            return false;
        }
        final Optional<int[]> origin = OmtBuildingBlitter.findClearOrigin(grid, footprint, clearableIds, rng);
        if (!origin.isPresent()) {
            addWarning(warnings, "no clear rect for city building " + building.getId());
            return false;
        }
        final int[] at = origin.get();
        final int pieceCount = OmtBuildingBlitter.blitAt(
            building, grid, at[0], at[1], 0, oterRegistry, warnings
        );
        if (pieceCount > 0 && placedCenters != null) {
            placedCenters.add(siteCenter(footprint, at[0], at[1]));
        }
        return pieceCount > 0;
    }

    private static int[] siteCenter(final BuildingFootprint footprint, final int baseX, final int baseY) {
        return new int[] {
            baseX + footprint.getMinOffsetX() + (footprint.getWidth() - 1) / 2,
            baseY + footprint.getMinOffsetY() + (footprint.getHeight() - 1) / 2
        };
    }

    private static List<CityBuildingDefinition> pickCandidates(
        final CityBuildingRegistry buildings,
        final OvermapGrid grid
    ) {
        final List<CityBuildingDefinition> candidates = new ArrayList<>();
        for (final CityBuildingDefinition building : buildings.all()) {
            if (building.isWholeOvermapSpecial()) {
                continue;
            }
            final BuildingFootprint footprint = BuildingFootprint.atZ(building, 0);
            if (footprint.isEmpty()) {
                continue;
            }
            if (footprint.getWidth() > grid.width() || footprint.getHeight() > grid.height()) {
                continue;
            }
            candidates.add(building);
        }
        return candidates;
    }

    private static void shuffle(final List<CityBuildingDefinition> candidates, final Random rng) {
        if (rng == null || candidates.size() <= 1) {
            return;
        }
        for (int i = candidates.size() - 1; i > 0; i--) {
            final int j = rng.nextInt(i + 1);
            Collections.swap(candidates, i, j);
        }
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
