package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OmLines;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.generate.RoadConnectionPolisher;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionGroundcoverSettings;

import java.util.Locale;
import java.util.Random;

/**
 * BN {@code mapgen_road} subset: NESW arms, yellow dashes, curve fillets, sidewalks (R2).
 */
public final class BuiltinRoadMapgen {

    private static final int SIZE = OmtStitchComposer.DEFAULT_OMT_SIZE;
    private static final int SEEX = SIZE / 2;
    private static final int SEEY = SIZE / 2;
    private static final String PAVEMENT = "t_pavement";
    private static final String PAVEMENT_Y = "t_pavement_y";
    private static final String SIDEWALK = "t_sidewalk";
    private static final String RUBBLE = "f_rubble";
    private static final String BENCH = "f_bench";

    private BuiltinRoadMapgen() {}

    public static MapGrid generate(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final String omtId,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapTerrainRegistry oterRegistry,
        final RegionGroundcoverSettings groundcover,
        final long previewSeed,
        final boolean placeLiteContent,
        final String mapExtraId
    ) {
        final MapGrid grid = new MapGrid(SIZE, SIZE, "t_grass");
        if (groundcover != null) {
            groundcover.applyPerCellBaseFill(grid, previewSeed, "t_grass");
        }
        final boolean[] roads = neswFromOmt(omtId, overmap, omtX, omtY, connectionRegistry);
        final int numDirs = count(roads);
        final int[] curve = curveHints(overmap, omtX, omtY, roads, connectionRegistry);
        final boolean[] sidewalks = sidewalkNeighbors(overmap, omtX, omtY, oterRegistry);
        final boolean anySidewalk = any(sidewalks);

        if (isDiagonal(roads)) {
            paintDiagonal(grid, roads, curve, anySidewalk);
        } else {
            paintOrthogonal(grid, roads, curve, sidewalks, numDirs);
        }

        if (placeLiteContent) {
            placeLiteContent(grid, previewSeed ^ (omtX * 31L + omtY), numDirs, anySidewalk);
        }
        if (mapExtraId != null && !mapExtraId.isEmpty()) {
            applyExtraStub(grid, mapExtraId, previewSeed);
        }
        return grid;
    }

    static boolean[] neswFromOmt(
        final String omtId,
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final OvermapConnectionRegistry connections
    ) {
        final boolean[] roads = new boolean[4];
        final String base = OmLines.stripToBase(omtId, "road", "test_road");
        final int line = base == null ? -1 : OmLines.bitsFromId(omtId, base);
        if (line >= 0) {
            roads[0] = OmLines.hasSegment(line, OmLines.NORTH);
            roads[1] = OmLines.hasSegment(line, OmLines.EAST);
            roads[2] = OmLines.hasSegment(line, OmLines.SOUTH);
            roads[3] = OmLines.hasSegment(line, OmLines.WEST);
            if (count(roads) > 0) {
                return roads;
            }
        }
        // Fallback: neighbor scan (pre-polish generic "road")
        roads[0] = neighborRoad(overmap, omtX, omtY - 1, connections);
        roads[1] = neighborRoad(overmap, omtX + 1, omtY, connections);
        roads[2] = neighborRoad(overmap, omtX, omtY + 1, connections);
        roads[3] = neighborRoad(overmap, omtX - 1, omtY, connections);
        if (omtId != null) {
            final String n = omtId.toLowerCase(Locale.ROOT);
            if (n.contains("hiway_ns") || n.endsWith("_ns")) {
                roads[0] = true;
                roads[2] = true;
            }
            if (n.contains("hiway_ew") || n.endsWith("_ew")) {
                roads[1] = true;
                roads[3] = true;
            }
        }
        if (count(roads) == 0) {
            roads[0] = roads[1] = roads[2] = roads[3] = true;
        }
        return roads;
    }

    private static void paintOrthogonal(
        final MapGrid grid,
        final boolean[] roads,
        final int[] curve,
        final boolean[] sidewalks,
        final int numDirs
    ) {
        final int deadEndExt = numDirs == 1 ? 8 : 0;
        for (int dir = 0; dir < 4; dir++) {
            if (!roads[dir]) {
                continue;
            }
            if (sidewalks[(dir + 3) % 4] || sidewalks[dir] || sidewalks[(dir + 3) % 4 + 4]) {
                fillRotatedRect(grid, SIDEWALK, 0, 0, 3, SEEY - 1 + deadEndExt, dir);
            }
            if (sidewalks[(dir + 1) % 4] || sidewalks[dir] || sidewalks[dir + 4]) {
                fillRotatedRect(grid, SIDEWALK, SIZE - 4, 0, SIZE - 1, SEEY - 1 + deadEndExt, dir);
            }
            fillRotatedRect(grid, PAVEMENT, 4, 0, SIZE - 5, SEEY - 1 + deadEndExt, dir);
            if (curve[dir] != 0) {
                for (int x = 1; x < 4; x++) {
                    for (int y = 0; y < x; y++) {
                        int tx = curve[dir] < 0 ? x : SIZE - 1 - x;
                        int ty = y;
                        final int[] p = rotateCw(tx, ty, dir);
                        grid.setTerrain(p[0], p[1], PAVEMENT);
                    }
                }
            }
            final int maxY = (numDirs == 4 || (numDirs == 3 && dir == 0)) ? 4 : SEEY;
            for (int x = SEEX - 1; x <= SEEX; x++) {
                for (int y = 0; y < maxY; y++) {
                    if ((y + dir / 2) % 4 != 0) {
                        final int[] p = rotateCw(x, y, dir);
                        if (PAVEMENT.equals(grid.get(p[0], p[1]).getTerrainId())) {
                            grid.setTerrain(p[0], p[1], PAVEMENT_Y);
                        }
                    }
                }
            }
        }
    }

    private static void paintDiagonal(
        final MapGrid grid,
        final boolean[] roads,
        final int[] curve,
        final boolean anySidewalk
    ) {
        if (anySidewalk) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (x > y - 4 && (x < 4 || y > SIZE - 5 || y >= x)) {
                        grid.setTerrain(x, y, SIDEWALK);
                    }
                }
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (x > y && ((x > 3 && y < SIZE - 4)
                    || (x < 4 && curve[0] < 0)
                    || (y > SIZE - 5 && curve[1] > 0))) {
                    if ((x % 4) != 0 && (x - y == SEEX - 1 || x - y == SEEX)) {
                        grid.setTerrain(x, y, PAVEMENT_Y);
                    } else {
                        grid.setTerrain(x, y, PAVEMENT);
                    }
                }
            }
        }
        // If corner is SW/NW/etc without NE orientation, rotate by painting all four diags via arms.
        if (!roads[0] || !roads[1]) {
            // Re-paint using orthogonal fallback for non-NE diagonals
            paintOrthogonal(grid, roads, curve, new boolean[8], 2);
        }
    }

    private static void placeLiteContent(
        final MapGrid grid,
        final long seed,
        final int numDirs,
        final boolean urban
    ) {
        final Random rng = new Random(seed);
        final int litter = urban ? 4 : 2;
        for (int i = 0; i < litter; i++) {
            if (!rng.nextBoolean()) {
                continue;
            }
            final int x = 2 + rng.nextInt(SIZE - 4);
            final int y = 2 + rng.nextInt(SIZE - 4);
            final String ter = grid.get(x, y).getTerrainId();
            if (PAVEMENT.equals(ter) || PAVEMENT_Y.equals(ter) || "t_grass".equals(ter)) {
                grid.setFurniture(x, y, RUBBLE);
            }
        }
        if (urban && numDirs >= 3 && rng.nextInt(3) == 0) {
            grid.setFurniture(SEEX, SEEY + 2, BENCH);
        }
    }

    private static void applyExtraStub(final MapGrid grid, final String extraId, final long seed) {
        final Random rng = new Random(seed ^ extraId.hashCode());
        final int x = 4 + rng.nextInt(SIZE - 8);
        final int y = 4 + rng.nextInt(SIZE - 8);
        if (extraId.contains("roadworks") || extraId.contains("roadblock") || extraId.contains("bandit")) {
            for (int dx = -1; dx <= 1; dx++) {
                grid.setFurniture(x + dx, y, "f_barricade_road");
            }
        } else if (extraId.contains("helicopter") || extraId.contains("aircraft") || extraId.contains("vehicle")) {
            grid.setFurniture(x, y, "f_wreckage");
            grid.setFurniture(x + 1, y, "f_wreckage");
        } else if (extraId.contains("crater")) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    grid.setTerrain(x + dx, y + dy, "t_dirt");
                }
            }
        } else {
            grid.setFurniture(x, y, RUBBLE);
        }
    }

    private static int[] curveHints(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final boolean[] roads,
        final OvermapConnectionRegistry connections
    ) {
        final int[] curve = new int[4];
        final int[][] delta = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        for (int dir = 0; dir < 4; dir++) {
            if (!roads[dir]) {
                continue;
            }
            final int nx = omtX + delta[dir][0];
            final int ny = omtY + delta[dir][1];
            if (overmap == null || nx < 0 || ny < 0 || nx >= overmap.width() || ny >= overmap.height()) {
                continue;
            }
            final String neighbor = overmap.getOmtId(nx, ny);
            final boolean[] nRoads = neswFromOmt(neighbor, overmap, nx, ny, connections);
            final int opposite = (dir + 2) % 4;
            if (count(nRoads) == 2 && nRoads[opposite]) {
                if (nRoads[(dir + 3) % 4]) {
                    curve[dir]--;
                }
                if (nRoads[(dir + 1) % 4]) {
                    curve[dir]++;
                }
            }
        }
        return curve;
    }

    private static boolean[] sidewalkNeighbors(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final OvermapTerrainRegistry registry
    ) {
        final boolean[] out = new boolean[8];
        final int[][] delta = {
            { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 },
            { 1, -1 }, { 1, 1 }, { -1, 1 }, { -1, -1 }
        };
        for (int i = 0; i < 8; i++) {
            final int x = omtX + delta[i][0];
            final int y = omtY + delta[i][1];
            out[i] = hasSidewalk(overmap, x, y, registry);
        }
        return out;
    }

    private static boolean hasSidewalk(
        final OvermapGrid overmap,
        final int x,
        final int y,
        final OvermapTerrainRegistry registry
    ) {
        if (overmap == null || x < 0 || y < 0 || x >= overmap.width() || y >= overmap.height()) {
            return false;
        }
        final String id = overmap.getOmtId(x, y);
        if (registry != null) {
            final OvermapTerrainDefinition def = registry.find(id).orElse(null);
            if (def != null) {
                for (final String flag : def.getFlags()) {
                    if ("SIDEWALK".equals(flag) || "HAS_SIDEWALK".equals(flag)) {
                        return true;
                    }
                }
            }
        }
        final String n = id == null ? "" : id.toLowerCase(Locale.ROOT);
        return n.contains("house") || n.contains("building") || n.contains("sidewalk");
    }

    private static boolean neighborRoad(
        final OvermapGrid overmap,
        final int x,
        final int y,
        final OvermapConnectionRegistry connections
    ) {
        if (overmap == null || x < 0 || y < 0 || x >= overmap.width() || y >= overmap.height()) {
            return false;
        }
        return RoadConnectionPolisher.isRoadFamily(overmap.getOmtId(x, y), connections);
    }

    private static boolean isDiagonal(final boolean[] roads) {
        final int n = count(roads);
        if (n != 2) {
            return false;
        }
        return (roads[0] && roads[1]) || (roads[1] && roads[2]) || (roads[2] && roads[3]) || (roads[3] && roads[0]);
    }

    private static void fillRotatedRect(
        final MapGrid grid,
        final String terrain,
        final int x1,
        final int y1,
        final int x2,
        final int y2,
        final int dir
    ) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                final int[] p = rotateCw(x, y, dir);
                if (p[0] >= 0 && p[1] >= 0 && p[0] < SIZE && p[1] < SIZE) {
                    grid.setTerrain(p[0], p[1], terrain);
                }
            }
        }
    }

    private static int[] rotateCw(final int x, final int y, final int rot) {
        int cx = x;
        int cy = y;
        for (int r = 0; r < Math.floorMod(rot, 4); r++) {
            final int temp = cy;
            cy = cx;
            cx = (SIZE - 1) - temp;
        }
        return new int[] { cx, cy };
    }

    private static int count(final boolean[] flags) {
        int n = 0;
        for (final boolean f : flags) {
            if (f) {
                n++;
            }
        }
        return n;
    }

    private static boolean any(final boolean[] flags) {
        for (final boolean f : flags) {
            if (f) {
                return true;
            }
        }
        return false;
    }
}
