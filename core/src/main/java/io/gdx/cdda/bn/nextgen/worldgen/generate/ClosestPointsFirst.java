package io.gdx.cdda.bn.nextgen.worldgen.generate;

import java.util.ArrayList;
import java.util.List;

/** Spiral point order from BN {@code closest_points_first} (point.cpp). */
final class ClosestPointsFirst {

    private ClosestPointsFirst() {}

    static List<int[]> spiral(final int centerX, final int centerY, final int maxDist) {
        return spiral(centerX, centerY, 0, maxDist);
    }

    static List<int[]> spiral(final int centerX, final int centerY, final int minDist, final int maxDist) {
        final List<int[]> points = new ArrayList<>();
        if (maxDist < 0 || minDist > maxDist) {
            return points;
        }
        final int minEdge = minDist * 2 + 1;
        final int maxEdge = maxDist * 2 + 1;
        final int count = maxEdge * maxEdge - (minEdge - 2) * (minEdge - 2);
        final boolean includeCenter = minDist == 0;

        int i = includeCenter ? -1 : 0;
        int x = Math.max(minDist, 1);
        int y = 1 - x;
        int dx = 1;
        int dy = 0;

        while (i < count) {
            if (i++ < 0) {
                points.add(new int[] { centerX, centerY });
                continue;
            }
            if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1 - y)) {
                final int swap = dx;
                dx = -dy;
                dy = swap;
            }
            x += dx;
            y += dy;
            points.add(new int[] { centerX + x, centerY + y });
        }
        return points;
    }
}
