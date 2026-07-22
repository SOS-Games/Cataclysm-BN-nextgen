package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.generate.UrbanTerrainClearables;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Default seed: former double-lane street around (77–78,74–75). */
class ParallelRoadSpotCheckTest {

    @Test
    void defaultSeedHasNoSolidTwoByTwoRoadBlockNear77_74() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGrid g = svc.generateOvermap(128, 128).getGrid();

        int blocks = 0;
        final StringBuilder locs = new StringBuilder();
        for (int y = 70; y < 80; y++) {
            for (int x = 72; x < 85; x++) {
                if (road(g, x, y) && road(g, x + 1, y) && road(g, x, y + 1) && road(g, x + 1, y + 1)) {
                    blocks++;
                    locs.append('(').append(x).append(',').append(y).append(")=")
                        .append(g.getOmtId(x, y)).append('/')
                        .append(g.getOmtId(x + 1, y)).append('/')
                        .append(g.getOmtId(x, y + 1)).append('/')
                        .append(g.getOmtId(x + 1, y + 1)).append('\n');
                }
            }
        }
        final StringBuilder grid = new StringBuilder();
        for (int y = 70; y <= 80; y++) {
            for (int x = 72; x <= 85; x++) {
                grid.append(road(g, x, y) ? 'R' : '.');
            }
            grid.append(" y=").append(y).append('\n');
        }
        assertEquals(0, blocks, "solid 2x2 road blocks:\n" + locs + grid);
    }

    private static boolean road(final OvermapGrid g, final int x, final int y) {
        return UrbanTerrainClearables.isRoadFamily(g.getOmtId(x, y));
    }
}
