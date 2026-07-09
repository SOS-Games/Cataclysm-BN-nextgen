package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import java.util.Random;
import java.util.Set;

/** BN-style drunkard walk between two OMT points (`overmap::place_river`). */
final class RiverDrunkardCarver {

    private RiverDrunkardCarver() {}

    static int carve(
        final OvermapGrid grid,
        final int startX,
        final int startY,
        final int endX,
        final int endY,
        final String riverCenterId,
        final String lakeId,
        final double riverScale,
        final Set<String> overwritableIds,
        final Random rng
    ) {
        final int riverChance = Math.max(1, (int) (1.0 / Math.max(0.01, riverScale)));
        final int brushRadius = Math.max(1, (int) riverScale);
        int painted = 0;
        int px = startX;
        int py = startY;
        final int width = grid.width();
        final int height = grid.height();
        int guard = width * height * 16;

        do {
            px = clamp(px + rng.nextInt(3) - 1, 0, width - 1);
            py = clamp(py + rng.nextInt(3) - 1, 0, height - 1);
            painted += paintBrush(grid, px, py, brushRadius, riverCenterId, lakeId, riverChance, overwritableIds, rng,
                width, height, false);

            if (endX > px && shouldStepToward(rng, endX - px, width, height, Math.abs(endY - py), true)) {
                px++;
            } else if (endX < px && shouldStepToward(rng, px - endX, width, height, Math.abs(endY - py), true)) {
                px--;
            }
            if (endY > py && shouldStepToward(rng, endY - py, height, width, Math.abs(px - endX), false)) {
                py++;
            } else if (endY < py && shouldStepToward(rng, py - endY, height, width, Math.abs(px - endX), false)) {
                py--;
            }

            px = clamp(px + rng.nextInt(3) - 1, 0, width - 2);
            py = clamp(py + rng.nextInt(3) - 1, 0, height - 1);
            painted += paintBrush(grid, px, py, brushRadius, riverCenterId, lakeId, riverChance, overwritableIds, rng,
                width, height, true);
        } while ((px != endX || py != endY) && guard-- > 0);

        return painted;
    }

    private static boolean shouldStepToward(
        final Random rng,
        final int delta,
        final int primarySpan,
        final int secondarySpan,
        final int secondaryDelta,
        final boolean primaryAxis
    ) {
        final int primaryCap = (int) (primarySpan * 1.2);
        final int secondaryCap = (int) (secondarySpan * 0.2);
        if (primaryCap > 0 && rng.nextInt(primaryCap) < delta) {
            return true;
        }
        return secondaryCap > 0
            && rng.nextInt(secondaryCap) > delta
            && rng.nextInt(secondaryCap) > secondaryDelta;
    }

    private static int paintBrush(
        final OvermapGrid grid,
        final int centerX,
        final int centerY,
        final int brushRadius,
        final String riverCenterId,
        final String lakeId,
        final int riverChance,
        final Set<String> overwritableIds,
        final Random rng,
        final int width,
        final int height,
        final boolean respectEdgeMargin
    ) {
        int painted = 0;
        for (int dy = -brushRadius; dy <= brushRadius; dy++) {
            for (int dx = -brushRadius; dx <= brushRadius; dx++) {
                final int x = centerX + dx;
                final int y = centerY + dy;
                if (x < 0 || y < 0 || x >= width || y >= height) {
                    continue;
                }
                if (respectEdgeMargin && (x < 1 || y < 1 || x >= width - 1 || y >= height - 1)) {
                    continue;
                }
                final String current = grid.getOmtId(x, y);
                if (lakeId != null && lakeId.equals(current)) {
                    continue;
                }
                if (!overwritableIds.contains(current)) {
                    continue;
                }
                if (rng.nextInt(riverChance) != 0) {
                    continue;
                }
                grid.setOmtId(x, y, riverCenterId);
                painted++;
            }
        }
        return painted;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
