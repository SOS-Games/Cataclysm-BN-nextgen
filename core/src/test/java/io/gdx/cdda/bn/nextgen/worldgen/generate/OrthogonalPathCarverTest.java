package io.gdx.cdda.bn.nextgen.worldgen.generate;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrthogonalPathCarverTest {

    @Test
    void meanderingPathCreatesMultipleBends() {
        final List<int[]> path = OrthogonalPathCarver.buildPath(0, 0, 20, 16, new Random(7L));
        assertEquals(0, path.get(0)[0]);
        assertEquals(0, path.get(0)[1]);
        final int[] last = path.get(path.size() - 1);
        assertEquals(20, last[0]);
        assertEquals(16, last[1]);
        assertTrue(countDirectionChanges(path) >= 3, "expected several bends, got " + countDirectionChanges(path));
    }

    @Test
    void deterministicPathWithoutRngIsSingleCornerOrStraight() {
        final List<int[]> path = OrthogonalPathCarver.buildPath(0, 0, 5, 3, null);
        final int[] last = path.get(path.size() - 1);
        assertEquals(5, last[0]);
        assertEquals(3, last[1]);
        assertTrue(countDirectionChanges(path) <= 1);
    }

    private static int countDirectionChanges(final List<int[]> path) {
        if (path.size() < 3) {
            return 0;
        }
        int changes = 0;
        int prevDx = path.get(1)[0] - path.get(0)[0];
        int prevDy = path.get(1)[1] - path.get(0)[1];
        for (int i = 2; i < path.size(); i++) {
            final int dx = path.get(i)[0] - path.get(i - 1)[0];
            final int dy = path.get(i)[1] - path.get(i - 1)[1];
            if (dx != prevDx || dy != prevDy) {
                changes++;
                prevDx = dx;
                prevDy = dy;
            }
        }
        return changes;
    }
}
