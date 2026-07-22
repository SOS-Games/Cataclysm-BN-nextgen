package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacementSource;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Places static {@code overmap_special} bundles (W4). */
public final class StaticSpecialPlacer {

    private StaticSpecialPlacer() {}

    public static int placeAll(
        final OvermapGrid grid,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapGenerateOptions options,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters,
        final List<PlacedBuildingRecord> placedBuildings
    ) {
        if (grid == null || buildings == null || options.getStaticSpecialQuota() <= 0) {
            return 0;
        }
        final Set<String> clearable = OmtBuildingBlitter.defaultClearableIds(options, oterRegistry);
        final List<CityBuildingDefinition> candidates = pickCandidates(buildings, grid);
        if (candidates.isEmpty()) {
            return 0;
        }
        shuffle(candidates, rng);

        int placed = 0;
        int attempts = 0;
        final int maxAttempts = options.getStaticSpecialQuota() * candidates.size() * 2;
        while (placed < options.getStaticSpecialQuota() && attempts < maxAttempts && !candidates.isEmpty()) {
            attempts++;
            final CityBuildingDefinition special = candidates.get(rng.nextInt(candidates.size()));
            if (CityPlacer.tryPlace(
                special,
                grid,
                oterRegistry,
                options,
                clearable,
                rng,
                warnings,
                placedCenters,
                placedBuildings,
                PlacementSource.STATIC_SPECIAL
            )) {
                placed++;
            }
        }
        return placed;
    }

    private static List<CityBuildingDefinition> pickCandidates(
        final CityBuildingRegistry buildings,
        final OvermapGrid grid
    ) {
        final List<CityBuildingDefinition> candidates = new ArrayList<>();
        for (final CityBuildingDefinition building : buildings.all()) {
            if (!building.isWholeOvermapSpecial()) {
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
}
