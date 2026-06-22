package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.MutableSpecialBuildingConverter;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.AssembledSpecialLayout;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.PlacedMutablePiece;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.SpecialPhaseAssembler;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.generate.MutableSpecialPlacer;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Places assembled mutable specials on an overmap grid (W6). */
public final class MutableSpecialPlacer {

    private MutableSpecialPlacer() {}

    public static int placeAll(
        final OvermapGrid grid,
        final MutableSpecialRegistry mutables,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapGenerateOptions options,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters,
        final List<PlacedBuildingRecord> placedBuildings
    ) {
        if (grid == null || mutables == null || options.getMutableSpecialQuota() <= 0) {
            return 0;
        }
        final List<MutableSpecialDefinition> candidates = pickCandidates(mutables, grid);
        if (candidates.isEmpty()) {
            return 0;
        }
        shuffle(candidates, rng);

        final Set<String> clearable = OmtBuildingBlitter.defaultClearableIds(options, oterRegistry);
        int placed = 0;
        int attempts = 0;
        final int maxAttempts = options.getMutableSpecialQuota() * candidates.size() * 2;
        while (placed < options.getMutableSpecialQuota() && attempts < maxAttempts && !candidates.isEmpty()) {
            attempts++;
            final MutableSpecialDefinition special = candidates.get(rng.nextInt(candidates.size()));
            if (tryPlace(special, grid, oterRegistry, clearable, rng, warnings, placedCenters, placedBuildings)) {
                placed++;
            }
        }
        return placed;
    }

    public static boolean tryPlace(
        final MutableSpecialDefinition special,
        final OvermapGrid grid,
        final OvermapTerrainRegistry oterRegistry,
        final Set<String> clearableIds,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters,
        final List<PlacedBuildingRecord> placedBuildings
    ) {
        if (special == null) {
            return false;
        }
        final Optional<AssembledSpecialLayout> assembled = SpecialPhaseAssembler.assemble(special, rng, warnings);
        if (!assembled.isPresent() || assembled.get().getPieces().size() < 2) {
            return false;
        }
        final AssembledSpecialLayout layout = assembled.get();
        final Optional<int[]> origin = findClearOrigin(grid, layout, clearableIds, rng);
        if (!origin.isPresent()) {
            addWarning(warnings, "no clear rect for mutable special " + special.getId());
            return false;
        }
        final int baseX = origin.get()[0] - layout.getMinOffsetX();
        final int baseY = origin.get()[1] - layout.getMinOffsetY();
        int blitted = 0;
        for (final PlacedMutablePiece piece : layout.getPieces()) {
            final int x = baseX + piece.getOffsetX();
            final int y = baseY + piece.getOffsetY();
            if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
                addWarning(warnings, "mutable piece out of bounds for " + special.getId());
                continue;
            }
            if (oterRegistry != null && !oterRegistry.contains(piece.getOvermapTerrainId())) {
                addWarning(warnings, "unknown overmap terrain '" + piece.getOvermapTerrainId() + "'");
            }
            grid.setOmtId(x, y, piece.resolveOvermapTerrainId());
            blitted++;
        }
        if (blitted > 0 && placedCenters != null) {
            placedCenters.add(new int[] {
                baseX + layout.getMinOffsetX() + (layout.getWidth() - 1) / 2,
                baseY + layout.getMinOffsetY() + (layout.getHeight() - 1) / 2
            });
        }
        if (blitted >= 2 && placedBuildings != null) {
            placedBuildings.add(PlacedBuildingRecord.ofMutable(
                MutableSpecialBuildingConverter.fromAssembledLayout(special, layout),
                baseX,
                baseY,
                layout
            ));
        }
        return blitted >= 2;
    }

    private static Optional<int[]> findClearOrigin(
        final OvermapGrid grid,
        final AssembledSpecialLayout layout,
        final Set<String> clearableIds,
        final Random rng
    ) {
        final List<int[]> candidates = new ArrayList<>();
        final int minOffsetX = layout.getMinOffsetX();
        final int minOffsetY = layout.getMinOffsetY();
        final int maxOffsetX = minOffsetX + layout.getWidth() - 1;
        final int maxOffsetY = minOffsetY + layout.getHeight() - 1;
        final int minBaseX = Math.max(0, -minOffsetX);
        final int minBaseY = Math.max(0, -minOffsetY);
        final int maxBaseX = grid.width() - 1 - maxOffsetX;
        final int maxBaseY = grid.height() - 1 - maxOffsetY;
        if (minBaseX > maxBaseX || minBaseY > maxBaseY) {
            return Optional.empty();
        }
        for (int baseY = minBaseY; baseY <= maxBaseY; baseY++) {
            for (int baseX = minBaseX; baseX <= maxBaseX; baseX++) {
                if (canPlaceAt(grid, layout, baseX, baseY, clearableIds)) {
                    candidates.add(new int[] { baseX + minOffsetX, baseY + minOffsetY });
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

    private static boolean canPlaceAt(
        final OvermapGrid grid,
        final AssembledSpecialLayout layout,
        final int baseX,
        final int baseY,
        final Set<String> clearableIds
    ) {
        for (final PlacedMutablePiece piece : layout.getPieces()) {
            final int x = baseX + piece.getOffsetX();
            final int y = baseY + piece.getOffsetY();
            if (!clearableIds.contains(grid.getOmtId(x, y))) {
                return false;
            }
        }
        return true;
    }

    private static List<MutableSpecialDefinition> pickCandidates(
        final MutableSpecialRegistry mutables,
        final OvermapGrid grid
    ) {
        final List<MutableSpecialDefinition> candidates = new ArrayList<>();
        for (final MutableSpecialDefinition special : mutables.all()) {
            if (special.getPhases().isEmpty()) {
                continue;
            }
            if (layoutTooLarge(special, grid)) {
                continue;
            }
            candidates.add(special);
        }
        return candidates;
    }

    private static boolean layoutTooLarge(
        final MutableSpecialDefinition special,
        final OvermapGrid grid
    ) {
        final Optional<AssembledSpecialLayout> layout = SpecialPhaseAssembler.assemble(
            special,
            null,
            null
        );
        if (!layout.isPresent()) {
            return true;
        }
        return layout.get().getWidth() > maxFootprint(grid) || layout.get().getHeight() > maxFootprint(grid);
    }

    private static int maxFootprint(final OvermapGrid grid) {
        final int side = Math.min(grid.width(), grid.height());
        if (side >= 128) {
            return 10;
        }
        if (side >= 64) {
            return 8;
        }
        if (side >= 32) {
            return 6;
        }
        return Math.max(2, side / 2);
    }

    private static void shuffle(final List<MutableSpecialDefinition> candidates, final Random rng) {
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
