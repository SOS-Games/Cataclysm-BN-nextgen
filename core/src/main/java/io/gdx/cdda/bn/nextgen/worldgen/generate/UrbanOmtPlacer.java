package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.CityContentWeights;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Places weighted 1×1 urban OMT ids inside a city blob (W17a). */
public final class UrbanOmtPlacer {

    private static final int SHOP_ROLL_PERCENT = 20;
    private static final int PARK_ROLL_PERCENT = 12;
    private static final int SKIP_LOT_PERCENT = 8;

    private UrbanOmtPlacer() {}

    public static int fillBlob(
        final OvermapGrid grid,
        final UrbanSite site,
        final CityContentWeights content,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry oterRegistry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || site == null || content == null || !content.hasUrbanOmtTables() || rng == null) {
            return 0;
        }
        final Set<String> clearable = OmtBuildingBlitter.defaultClearableIds(options, oterRegistry);
        final Set<String> warnedTokens = new HashSet<>();
        int placed = 0;
        boolean finalePlaced = false;
        final int finaleMinDistance = site.getTier().isAttemptFinale() ? Math.max(2, site.getRadius() / 2) : 0;

        for (int dy = -site.getRadius(); dy <= site.getRadius(); dy++) {
            for (int dx = -site.getRadius(); dx <= site.getRadius(); dx++) {
                final int x = site.getCenterX() + dx;
                final int y = site.getCenterY() + dy;
                if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
                    continue;
                }
                if (!isClearableCell(grid, x, y, clearable, options)) {
                    continue;
                }
                if (x == site.getCenterX() && y == site.getCenterY()) {
                    continue;
                }
                final int distance = Math.abs(dx) + Math.abs(dy);
                if (!finalePlaced
                    && site.getTier().isAttemptFinale()
                    && content.getFinales() != null
                    && !content.getFinales().isEmpty()
                    && distance >= finaleMinDistance
                    && rng.nextInt(6) == 0) {
                    final Optional<String> finale = WeightedOmtPicker.pick(
                        content.getFinales(),
                        oterRegistry,
                        rng,
                        warnedTokens,
                        warnings,
                        "unknown city finale OMT: "
                    );
                    if (finale.isPresent()) {
                        grid.setOmtId(x, y, finale.get());
                        placed++;
                        finalePlaced = true;
                        continue;
                    }
                }
                final UrbanCategory category = rollCategory(rng, content);
                if (category == UrbanCategory.SKIP) {
                    continue;
                }
                final Optional<String> omtId = pickForCategory(
                    category,
                    content,
                    oterRegistry,
                    rng,
                    warnedTokens,
                    warnings
                );
                if (omtId.isPresent()) {
                    grid.setOmtId(x, y, omtId.get());
                    placed++;
                }
            }
        }
        return placed;
    }

    private enum UrbanCategory {
        HOUSE,
        SHOP,
        PARK,
        SKIP
    }

    private static UrbanCategory rollCategory(final Random rng, final CityContentWeights content) {
        final int roll = rng.nextInt(100);
        if (roll < SKIP_LOT_PERCENT) {
            return UrbanCategory.SKIP;
        }
        if (roll < SKIP_LOT_PERCENT + PARK_ROLL_PERCENT && !content.getParks().isEmpty()) {
            return UrbanCategory.PARK;
        }
        if (roll < SKIP_LOT_PERCENT + PARK_ROLL_PERCENT + SHOP_ROLL_PERCENT && !content.getShops().isEmpty()) {
            return UrbanCategory.SHOP;
        }
        if (!content.getHouses().isEmpty()) {
            return UrbanCategory.HOUSE;
        }
        if (!content.getShops().isEmpty()) {
            return UrbanCategory.SHOP;
        }
        if (!content.getParks().isEmpty()) {
            return UrbanCategory.PARK;
        }
        return UrbanCategory.SKIP;
    }

    private static Optional<String> pickForCategory(
        final UrbanCategory category,
        final CityContentWeights content,
        final OvermapTerrainRegistry oterRegistry,
        final Random rng,
        final Set<String> warnedTokens,
        final List<String> warnings
    ) {
        switch (category) {
            case SHOP:
                return WeightedOmtPicker.pick(
                    content.getShops(),
                    oterRegistry,
                    rng,
                    warnedTokens,
                    warnings,
                    "unknown city shop OMT: "
                );
            case PARK:
                return WeightedOmtPicker.pick(
                    content.getParks(),
                    oterRegistry,
                    rng,
                    warnedTokens,
                    warnings,
                    "unknown city park OMT: "
                );
            case HOUSE:
            default:
                return WeightedOmtPicker.pick(
                    content.getHouses(),
                    oterRegistry,
                    rng,
                    warnedTokens,
                    warnings,
                    "unknown city house OMT: "
                );
        }
    }

    private static boolean isClearableCell(
        final OvermapGrid grid,
        final int x,
        final int y,
        final Set<String> clearable,
        final OvermapGenerateOptions options
    ) {
        final String existing = grid.getOmtId(x, y);
        if (existing == null || existing.isEmpty()) {
            return false;
        }
        if (!clearable.contains(existing)) {
            return false;
        }
        if (options != null) {
            if (matchesProtectedId(existing, options.getRiverCenterId())
                || matchesProtectedId(existing, options.getRiverBankId())
                || matchesProtectedId(existing, options.getLakeId())) {
                return false;
            }
            if (matchesProtectedId(existing, "test_river")
                || matchesProtectedId(existing, "test_lake")
                || matchesProtectedId(existing, "test_swamp")
                || matchesProtectedId(existing, "test_beach")) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesProtectedId(final String existing, final String candidate) {
        return candidate != null && !candidate.isEmpty() && candidate.equals(existing);
    }
}
