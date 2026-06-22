package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Writes city building / special OMT ids onto an {@link OvermapGrid} (W4). */
public final class OmtBuildingBlitter {

    private OmtBuildingBlitter() {}

    public static int blitAt(
        final CityBuildingDefinition building,
        final OvermapGrid grid,
        final int baseX,
        final int baseY,
        final int zLevel,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        final BuildingFootprint footprint = BuildingFootprint.atZ(building, zLevel);
        if (footprint.isEmpty()) {
            return 0;
        }
        int placed = 0;
        for (final CityBuildingPiece piece : footprint.getPieces()) {
            final int x = baseX + piece.getOffsetX();
            final int y = baseY + piece.getOffsetY();
            if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
                addWarning(warnings, "skipped out-of-bounds piece for " + building.getId() + " at (" + x + "," + y + ")");
                continue;
            }
            final String omtId = piece.getOvermapId();
            if (registry != null && !registry.contains(omtId)) {
                addWarning(warnings, "unknown overmap terrain '" + omtId + "' on " + building.getId());
            }
            grid.setOmtId(x, y, omtId);
            placed++;
        }
        return placed;
    }

    public static Optional<int[]> findClearOrigin(
        final OvermapGrid grid,
        final BuildingFootprint footprint,
        final Set<String> clearableIds,
        final Random rng
    ) {
        if (footprint.isEmpty()) {
            return Optional.empty();
        }
        final List<int[]> candidates = new ArrayList<>();
        final int minBaseX = Math.max(0, -footprint.getMinOffsetX());
        final int minBaseY = Math.max(0, -footprint.getMinOffsetY());
        final int maxBaseX = grid.width() - 1 - maxOffset(footprint.getPieces(), true);
        final int maxBaseY = grid.height() - 1 - maxOffset(footprint.getPieces(), false);
        if (minBaseX > maxBaseX || minBaseY > maxBaseY) {
            return Optional.empty();
        }
        for (int y = minBaseY; y <= maxBaseY; y++) {
            for (int x = minBaseX; x <= maxBaseX; x++) {
                if (canPlaceAt(grid, x, y, footprint, clearableIds)) {
                    candidates.add(new int[] { x, y });
                }
            }
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (rng == null) {
            return Optional.of(candidates.get(0));
        }
        return Optional.of(candidates.get(rng.nextInt(candidates.size())));
    }

    public static Optional<int[]> findClearOriginNearSites(
        final OvermapGrid grid,
        final BuildingFootprint footprint,
        final Set<String> clearableIds,
        final List<int[]> citySites,
        final int citySize,
        final Random rng
    ) {
        if (footprint.isEmpty() || citySites == null || citySites.isEmpty()) {
            return findClearOrigin(grid, footprint, clearableIds, rng);
        }
        final List<int[]> candidates = new ArrayList<>();
        final List<int[]> shuffledSites = new ArrayList<>(citySites);
        if (rng != null) {
            for (int i = shuffledSites.size() - 1; i > 0; i--) {
                final int j = rng.nextInt(i + 1);
                java.util.Collections.swap(shuffledSites, i, j);
            }
        }
        for (final int[] site : shuffledSites) {
            final int radius = Math.max(1, citySize);
            for (int attempt = 0; attempt < radius * radius * 2; attempt++) {
                final int baseX = site[0] + (rng == null ? 0 : rng.nextInt(radius * 2 + 1) - radius);
                final int baseY = site[1] + (rng == null ? 0 : rng.nextInt(radius * 2 + 1) - radius);
                if (!CitySitePicker.isWithinCityBlob(citySites, citySize, baseX, baseY)) {
                    continue;
                }
                if (canPlaceAt(grid, baseX, baseY, footprint, clearableIds)) {
                    candidates.add(new int[] { baseX, baseY });
                }
            }
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (rng == null) {
            return Optional.of(candidates.get(0));
        }
        return Optional.of(candidates.get(rng.nextInt(candidates.size())));
    }

    public static Set<String> defaultClearableIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final Set<String> ids = new HashSet<>();
        if (options != null) {
            ids.add(options.getFieldId());
            ids.add(options.getForestId());
        }
        addIfPresent(ids, "open_air", registry);
        if (ids.isEmpty()) {
            ids.add("field");
        }
        return Collections.unmodifiableSet(ids);
    }

    private static void addIfPresent(
        final Set<String> ids,
        final String id,
        final OvermapTerrainRegistry registry
    ) {
        if (id == null || id.isEmpty()) {
            return;
        }
        if (registry == null || registry.contains(id)) {
            ids.add(id);
        }
    }

    private static boolean canPlaceAt(
        final OvermapGrid grid,
        final int baseX,
        final int baseY,
        final BuildingFootprint footprint,
        final Set<String> clearableIds
    ) {
        for (final CityBuildingPiece piece : footprint.getPieces()) {
            final int x = baseX + piece.getOffsetX();
            final int y = baseY + piece.getOffsetY();
            if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
                return false;
            }
            if (!clearableIds.contains(grid.getOmtId(x, y))) {
                return false;
            }
        }
        return true;
    }

    private static int maxOffset(final List<CityBuildingPiece> pieces, final boolean xAxis) {
        int max = Integer.MIN_VALUE;
        for (final CityBuildingPiece piece : pieces) {
            max = Math.max(max, xAxis ? piece.getOffsetX() : piece.getOffsetY());
        }
        return max;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
