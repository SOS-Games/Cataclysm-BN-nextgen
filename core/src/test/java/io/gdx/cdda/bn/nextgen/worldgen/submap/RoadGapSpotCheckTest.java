package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.generate.UrbanTerrainClearables;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Default seed: field islands ringed by unconnected road stubs. */
class RoadGapSpotCheckTest {

    @Test
    void defaultSeedHasNoNaturalHolesWithThreeRoadNeighbors() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGrid g = svc.generateOvermap(128, 128).getGrid();

        int holes = 0;
        final StringBuilder locs = new StringBuilder();
        for (int y = 1; y < g.height() - 1; y++) {
            for (int x = 1; x < g.width() - 1; x++) {
                final String id = g.getOmtId(x, y);
                if (id == null) {
                    continue;
                }
                final String n = id.toLowerCase();
                if (!(n.equals("field") || n.contains("forest") || n.contains("swamp"))) {
                    continue;
                }
                if (n.startsWith("forest_trail") || n.startsWith("trailhead")) {
                    continue;
                }
                int roads = 0;
                if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x, y - 1))) {
                    roads++;
                }
                if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x + 1, y))) {
                    roads++;
                }
                if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x, y + 1))) {
                    roads++;
                }
                if (UrbanTerrainClearables.isRoadFamily(g.getOmtId(x - 1, y))) {
                    roads++;
                }
                if (roads >= 3) {
                    holes++;
                    if (holes <= 8) {
                        locs.append('(').append(x).append(',').append(y).append(")=")
                            .append(id).append(" roads=").append(roads).append('\n');
                    }
                }
            }
        }
        assertTrue(holes == 0, "natural holes with >=3 road neighbors=" + holes + "\n" + locs);
    }
}
