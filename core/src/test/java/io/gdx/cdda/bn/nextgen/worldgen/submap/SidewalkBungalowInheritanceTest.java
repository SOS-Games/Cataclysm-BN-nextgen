package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateResult;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression: bungalow10 inherits SIDEWALK; road west strip continuous beside house. */
class SidewalkBungalowInheritanceTest {

    @Test
    void bungalow10InheritsSidewalkFlagFromCopyFrom() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapTerrainDefinition bungalow = svc.getOvermapTerrainRegistry()
            .find("bungalow10")
            .orElseThrow();
        assertTrue(
            bungalow.getFlags().contains("SIDEWALK"),
            "bungalow10 should inherit SIDEWALK via copy-from, got " + bungalow.getFlags()
        );
    }

    @Test
    void defaultSeedRoadBesideSidewalkBuildingHasFullWestSidewalk() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGenerateResult result = svc.generateOvermap(128, 128);
        final OvermapGrid g = result.getGrid();

        // Prefer the historically reported spot; otherwise scan for a NS road with a
        // SIDEWALK-flagged western neighbor.
        int roadX = 70;
        int roadY = 67;
        final String west69 = g.getOmtId(69, 67);
        if (west69 == null || !hasSidewalkFlag(svc, west69) || !g.getOmtId(70, 67).contains("road")) {
            boolean found = false;
            outer:
            for (int y = 1; y < g.height() - 1; y++) {
                for (int x = 1; x < g.width() - 1; x++) {
                    final String roadId = g.getOmtId(x, y);
                    if (roadId == null || !roadId.contains("road")) {
                        continue;
                    }
                    if (!hasSidewalkFlag(svc, g.getOmtId(x - 1, y))) {
                        continue;
                    }
                    roadX = x;
                    roadY = y;
                    found = true;
                    break outer;
                }
            }
            assertTrue(found, "expected a road with SIDEWALK neighbor to the west");
        }

        final String roadId = g.getOmtId(roadX, roadY);
        final MapGrid road = BuiltinRoadMapgen.generate(
            g,
            roadX,
            roadY,
            roadId,
            svc.getOvermapConnectionRegistry(),
            svc.getOvermapTerrainRegistry(),
            null,
            12345L,
            false,
            null
        );

        int northSw = 0;
        int southSw = 0;
        for (int y = 0; y < 12; y++) {
            if ("t_sidewalk".equals(road.get(2, y).getTerrainId())) {
                northSw++;
            }
        }
        for (int y = 12; y < 24; y++) {
            if ("t_sidewalk".equals(road.get(2, y).getTerrainId())) {
                southSw++;
            }
        }
        assertTrue(northSw >= 8, "north half west strip should be sidewalk at ("
            + roadX + "," + roadY + ") west=" + g.getOmtId(roadX - 1, roadY) + " got " + northSw);
        assertTrue(southSw >= 8, "south half west strip should be sidewalk, got " + southSw);
    }

    private static boolean hasSidewalkFlag(final WorldgenPreviewService svc, final String id) {
        if (id == null) {
            return false;
        }
        OvermapTerrainDefinition def = svc.getOvermapTerrainRegistry().find(id).orElse(null);
        if (def == null) {
            final String base = id.replaceAll("_(north|east|south|west)$", "");
            def = svc.getOvermapTerrainRegistry().find(base).orElse(null);
        }
        if (def == null) {
            return false;
        }
        return def.getFlags().contains("SIDEWALK");
    }
}
