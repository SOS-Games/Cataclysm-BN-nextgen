package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateResult;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionGroundcoverSettings;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Default seed (12345) spots reported for sidewalk / curve issues.
 */
class SidewalkSpotCheckTest {

    @Test
    void defaultSeedRoadWestOf77_72HasContinuousWestSidewalk() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGenerateResult result = svc.generateOvermap(128, 128);
        final OvermapGrid g = result.getGrid();

        // Surface sewers must not punch the street beside this cell.
        assertFalse(
            g.getOmtId(76, 72).contains("sewer"),
            "expected road at 76,72, got " + g.getOmtId(76, 72)
        );
        assertTrue(
            g.getOmtId(76, 72).contains("road"),
            "expected road at 76,72, got " + g.getOmtId(76, 72)
        );
        // LINEAR polish should emit a directional id (not bare "road").
        assertFalse(
            "road".equals(g.getOmtId(76, 72)),
            "expected polished LINEAR road id at 76,72, got bare road"
        );

        final MapGrid road = BuiltinRoadMapgen.generate(
            g,
            76,
            72,
            g.getOmtId(76, 72),
            svc.getOvermapConnectionRegistry(),
            svc.getOvermapTerrainRegistry(),
            RegionGroundcoverSettings.defaults(),
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
        assertTrue(northSw >= 8, "west strip north half sidewalk, got " + northSw
            + " id=" + g.getOmtId(76, 72)
            + " W=" + g.getOmtId(75, 72)
            + " NW=" + g.getOmtId(75, 71)
            + " SW=" + g.getOmtId(75, 73)
            + " E=" + g.getOmtId(77, 72));
        assertTrue(southSw >= 8, "west strip south half sidewalk, got " + southSw);
    }

    @Test
    void defaultSeedCurvedRoadsNear77_72UseDirectionalIds() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGrid g = svc.generateOvermap(128, 128).getGrid();

        for (final int[] p : new int[][] { { 76, 71 }, { 76, 72 }, { 78, 72 }, { 77, 74 } }) {
            final String id = g.getOmtId(p[0], p[1]);
            if (id == null || !id.contains("road")) {
                continue;
            }
            assertFalse(
                "road".equals(id),
                "(" + p[0] + "," + p[1] + ") should be polished LINEAR id, got bare road"
            );
        }
    }

    @Test
    void defaultSeed81_76TopRightAbutsRoadWithSidewalk() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGrid g = svc.generateOvermap(128, 128).getGrid();

        // Top-right of 81,76 faces the road at 82,76 (and often a lot at 81,75).
        final String east = g.getOmtId(82, 76);
        assertTrue(east.contains("road"), "east of 81,76 should be road, got " + east);

        final MapGrid road = BuiltinRoadMapgen.generate(
            g,
            82,
            76,
            east,
            svc.getOvermapConnectionRegistry(),
            svc.getOvermapTerrainRegistry(),
            RegionGroundcoverSettings.defaults(),
            12345L,
            false,
            null
        );
        int westSw = 0;
        int northSw = 0;
        for (int y = 0; y < 24; y++) {
            if ("t_sidewalk".equals(road.get(2, y).getTerrainId())) {
                westSw++;
            }
        }
        for (int x = 0; x < 24; x++) {
            if ("t_sidewalk".equals(road.get(x, 2).getTerrainId())) {
                northSw++;
            }
        }
        assertTrue(
            westSw >= 8 || northSw >= 8,
            "road at 82,76 should show sidewalk toward 81,76 corner (westSw=" + westSw + " northSw=" + northSw + ")"
        );
    }

    @Test
    void defaultSeed74_59BottomLeftGetsSidewalkOnAdjacentRoads() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGrid g = svc.generateOvermap(128, 128).getGrid();

        // Layout may place a lot instead of a field hole; still require street-edge sidewalks.
        final String westId = g.getOmtId(73, 59);
        Assumptions.assumeTrue(
            westId != null && westId.contains("road"),
            "west of 74,59 not a road in this layout: " + westId
        );
        final MapGrid west = BuiltinRoadMapgen.generate(
            g, 73, 59, westId,
            svc.getOvermapConnectionRegistry(), svc.getOvermapTerrainRegistry(),
            RegionGroundcoverSettings.defaults(), 12345L, false, null
        );
        int eastSouth = 0;
        for (int y = 12; y < 24; y++) {
            if ("t_sidewalk".equals(west.get(21, y).getTerrainId())) {
                eastSouth++;
            }
        }
        assertTrue(eastSouth >= 8, "road 73,59 east strip south half sidewalk, got " + eastSouth
            + " E=" + g.getOmtId(74, 59) + " SE=" + g.getOmtId(74, 60));

        final String southId = g.getOmtId(74, 60);
        Assumptions.assumeTrue(
            southId != null && southId.contains("road"),
            "south of 74,59 not a road in this layout: " + southId
        );
        final MapGrid south = BuiltinRoadMapgen.generate(
            g, 74, 60, southId,
            svc.getOvermapConnectionRegistry(), svc.getOvermapTerrainRegistry(),
            RegionGroundcoverSettings.defaults(), 12345L, false, null
        );
        int northWest = 0;
        for (int x = 0; x < 12; x++) {
            if ("t_sidewalk".equals(south.get(x, 2).getTerrainId())) {
                northWest++;
            }
        }
        assertTrue(northWest >= 8, "road 74,60 north strip west half sidewalk, got " + northWest
            + " N=" + g.getOmtId(74, 59) + " NW=" + g.getOmtId(73, 59));
    }

    @Test
    void defaultSeed83_77AreaRoadHasSomeSidewalk() throws Exception {
        final WorldgenPreviewService svc = new WorldgenPreviewService();
        Assumptions.assumeTrue(svc.hasDataRoots(), "BN data roots required");
        svc.setWorldSeed(12345L);
        svc.ensureLoaded(WorldgenScanOptions.defaults());
        final OvermapGrid g = svc.generateOvermap(128, 128).getGrid();

        int rx = -1;
        int ry = -1;
        for (int y = 75; y <= 79 && rx < 0; y++) {
            for (int x = 80; x <= 85; x++) {
                final String id = g.getOmtId(x, y);
                if (id != null && id.contains("road")) {
                    rx = x;
                    ry = y;
                    break;
                }
            }
        }
        Assumptions.assumeTrue(rx >= 0, "no road near 83,77");
        final MapGrid road = BuiltinRoadMapgen.generate(
            g, rx, ry, g.getOmtId(rx, ry),
            svc.getOvermapConnectionRegistry(), svc.getOvermapTerrainRegistry(),
            RegionGroundcoverSettings.defaults(), 12345L, false, null
        );
        int sidewalk = 0;
        for (int y = 0; y < 24; y++) {
            for (final int x : new int[] { 2, 21 }) {
                if ("t_sidewalk".equals(road.get(x, y).getTerrainId())) {
                    sidewalk++;
                }
            }
        }
        for (int x = 0; x < 24; x++) {
            for (final int y : new int[] { 2, 21 }) {
                if ("t_sidewalk".equals(road.get(x, y).getTerrainId())) {
                    sidewalk++;
                }
            }
        }
        assertTrue(
            sidewalk >= 8,
            "road near 83,77 at " + rx + "," + ry + " should carry sidewalk, got " + sidewalk
                + " id=" + g.getOmtId(rx, ry)
        );
    }
}
