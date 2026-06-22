package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacementSource;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapSpecialSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Places region-weighted static {@code overmap_special} bundles (W14a). */
public final class RegionSpecialPlacer {

    private RegionSpecialPlacer() {}

    public static int placeAll(
        final OvermapGrid grid,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters,
        final List<PlacedBuildingRecord> placedBuildings
    ) {
        if (grid == null || buildings == null || region == null || rng == null) {
            return 0;
        }
        final OvermapSpecialSettings settings = region.getSpecialSettings();
        if (!settings.isEnabled()) {
            return 0;
        }
        final Set<String> clearable = OmtBuildingBlitter.defaultClearableIds(options, oterRegistry);
        final Set<String> warnedTokens = new HashSet<>();
        final int targetCount = rollPlacementCount(settings, rng);
        int placed = 0;
        int attempts = 0;
        final int maxAttempts = Math.max(targetCount, 1) * settings.getWeightedSpecials().size() * 4;
        while (placed < targetCount && attempts < maxAttempts) {
            attempts++;
            final Optional<String> specialId = settings.pickWeightedSpecial(rng);
            if (!specialId.isPresent()) {
                break;
            }
            final Optional<CityBuildingDefinition> building = buildings.findById(specialId.get());
            if (!building.isPresent() || !building.get().isWholeOvermapSpecial()) {
                if (warnedTokens.add(specialId.get())) {
                    addWarning(warnings, "unknown region special: " + specialId.get());
                }
                continue;
            }
            if (CityPlacer.tryPlace(
                building.get(),
                grid,
                oterRegistry,
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

    private static int rollPlacementCount(final OvermapSpecialSettings settings, final Random rng) {
        final int min = settings.getMinCount();
        final int max = settings.getMaxCount();
        if (max <= min) {
            return max;
        }
        return min + rng.nextInt(max - min + 1);
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
