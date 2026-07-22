package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionGroundcoverSettings;

import java.util.Locale;

/**
 * BN {@code mapgen_highway}: wide paved strip with side railings and yellow dashes.
 * Default orientation is north–south; {@code hiway_ew} is rotated 90° CW.
 */
public final class BuiltinHighwayMapgen {

    private static final int SIZE = OmtStitchComposer.DEFAULT_OMT_SIZE;
    private static final int SEEX = SIZE / 2;
    private static final String PAVEMENT = "t_pavement";
    private static final String PAVEMENT_Y = "t_pavement_y";
    private static final String RAILING = "t_railing";
    private static final String FALLBACK_RAILING = "t_fence";

    private BuiltinHighwayMapgen() {}

    public static boolean isHighwayOmt(final String omtId) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        return n.equals("highway")
            || n.startsWith("hiway_")
            || n.equals("test_hiway_ns")
            || n.equals("test_hiway_ew")
            || n.startsWith("test_hiway_");
    }

    public static MapGrid generate(
        final String omtId,
        final RegionGroundcoverSettings groundcover,
        final long previewSeed
    ) {
        final MapGrid grid = new MapGrid(SIZE, SIZE, "t_grass");
        if (groundcover != null) {
            groundcover.applyPerCellBaseFill(grid, previewSeed, "t_grass");
        }
        paintNorthSouthHighway(grid);
        if (isEastWest(omtId)) {
            return MapGridRotator.rotate(grid, 1);
        }
        return grid;
    }

    private static boolean isEastWest(final String omtId) {
        if (omtId == null) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        return n.contains("hiway_ew") || n.endsWith("_ew") && n.contains("hiway");
    }

    private static void paintNorthSouthHighway(final MapGrid grid) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (i < 3 || i >= SIZE - 3) {
                    // leave groundcover
                    continue;
                }
                if (i == 3 || i == SIZE - 4) {
                    grid.setTerrain(i, j, RAILING);
                    continue;
                }
                if ((i == SEEX - 1 || i == SEEX) && j % 4 != 0) {
                    grid.setTerrain(i, j, PAVEMENT_Y);
                } else {
                    grid.setTerrain(i, j, PAVEMENT);
                }
            }
        }
    }

    /** Prefer railing id when present in a tileset / terrain registry consumer. */
    static String railingId() {
        return RAILING;
    }

    static String fallbackRailingId() {
        return FALLBACK_RAILING;
    }
}
