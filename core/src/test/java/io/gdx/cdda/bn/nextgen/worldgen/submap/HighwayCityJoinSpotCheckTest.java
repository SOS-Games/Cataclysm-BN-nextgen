package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.generate.UrbanTerrainClearables;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Default seed: highway / city-road stubs should not sit one cell apart across a field gap.
 */
class HighwayCityJoinSpotCheckTest {

    @Test
    void defaultSeedHasNoOrphanRoadTipsFacingNaturalHolesNearReportedJoins() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGrid g = svc.generateOvermap(128, 128).getGrid();

        // Reported join neighborhoods: (58,56)/(59,55) and (23,7)/(23,9).
        assertTrue(
            noTipFacingHoleInWindow(g, 54, 52, 64, 60),
            "orphan road tip facing a hole near 58,56"
        );
        assertTrue(
            noDiagonalNearMissInWindow(g, 54, 52, 64, 60),
            "diagonal highway/city near-miss near 58,56"
        );
        assertTrue(
            noTipFacingHoleInWindow(g, 19, 5, 28, 12),
            "orphan road tip facing a hole near 23,7–9"
        );
    }

    private static boolean noDiagonalNearMissInWindow(
        final OvermapGrid g,
        final int x0,
        final int y0,
        final int x1,
        final int y1
    ) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!isRoadTip(g, x, y)) {
                    continue;
                }
                final int[] out = outwardDelta(g, x, y);
                if (out == null) {
                    continue;
                }
                final int hx = x + out[0];
                final int hy = y + out[1];
                if (hx < 0 || hy < 0 || hx >= g.width() || hy >= g.height()) {
                    continue;
                }
                if (!isNaturalHole(g.getOmtId(hx, hy))) {
                    continue;
                }
                final int[][] perps = out[0] == 0
                    ? new int[][] { { 1, 0 }, { -1, 0 } }
                    : new int[][] { { 0, 1 }, { 0, -1 } };
                for (final int[] perp : perps) {
                    final int rx = hx + perp[0];
                    final int ry = hy + perp[1];
                    if (rx >= 0 && ry >= 0 && rx < g.width() && ry < g.height()
                        && UrbanTerrainClearables.isRoadFamily(g.getOmtId(rx, ry))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean noTipFacingHoleInWindow(
        final OvermapGrid g,
        final int x0,
        final int y0,
        final int x1,
        final int y1
    ) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!isRoadTip(g, x, y)) {
                    continue;
                }
                final int[] out = outwardDelta(g, x, y);
                if (out == null) {
                    continue;
                }
                final int nx = x + out[0];
                final int ny = y + out[1];
                if (nx < 0 || ny < 0 || nx >= g.width() || ny >= g.height()) {
                    continue;
                }
                final String ahead = g.getOmtId(nx, ny);
                if (isNaturalHole(ahead)) {
                    // One more step: if a road sits beyond the hole, this is an unbridged gap.
                    final int nx2 = nx + out[0];
                    final int ny2 = ny + out[1];
                    if (nx2 >= 0 && ny2 >= 0 && nx2 < g.width() && ny2 < g.height()
                        && UrbanTerrainClearables.isRoadFamily(g.getOmtId(nx2, ny2))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isRoadTip(final OvermapGrid g, final int x, final int y) {
        if (!UrbanTerrainClearables.isRoadFamily(g.getOmtId(x, y))) {
            return false;
        }
        int n = 0;
        if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x, y - 1))) {
            n++;
        }
        if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x + 1, y))) {
            n++;
        }
        if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x, y + 1))) {
            n++;
        }
        if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x - 1, y))) {
            n++;
        }
        return n == 1;
    }

    private static int[] outwardDelta(final OvermapGrid g, final int x, final int y) {
        final int[][] d = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        for (final int[] p : d) {
            if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x + p[0], y + p[1]))) {
                return new int[] { -p[0], -p[1] };
            }
        }
        return null;
    }

    private static boolean isNaturalHole(final String id) {
        if (id == null) {
            return false;
        }
        final String n = id.toLowerCase();
        return n.equals("field") || n.contains("forest") || n.contains("swamp") || n.equals("open_air");
    }
}
