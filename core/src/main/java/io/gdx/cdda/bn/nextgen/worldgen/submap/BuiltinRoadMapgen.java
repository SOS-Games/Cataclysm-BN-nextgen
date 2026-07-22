package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
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
 * Canonicalizes orientation like BN ({@code rot}/{@code diag}), paints, then rotates the grid.
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
        MapGrid grid = new MapGrid(SIZE, SIZE, "t_grass");
        if (groundcover != null) {
            groundcover.applyPerCellBaseFill(grid, previewSeed, "t_grass");
        }
        final boolean[] roads = neswFromOmt(omtId, overmap, omtX, omtY, connectionRegistry);
        final int numDirs = count(roads);
        final int[] curve = curveHints(overmap, omtX, omtY, roads, connectionRegistry);
        final boolean[] sidewalks = sidewalkNeighbors(overmap, omtX, omtY, oterRegistry);
        final boolean anySidewalk = any(sidewalks);

        final Orientation orient = canonicalize(roads, numDirs);
        // BN nesw_array_rotate: 4-dir arrays by rot; 8-dir sidewalks by rot*2 (45° steps).
        neswArrayRotate(roads, orient.rot);
        neswArrayRotate(curve, orient.rot);
        neswArrayRotate(sidewalks, orient.rot * 2);

        if (orient.diag) {
            paintDiagonalCanonical(grid, curve, sidewalks);
        } else {
            paintOrthogonalCanonical(grid, roads, curve, sidewalks, numDirs, orient.rot);
        }

        if (placeLiteContent) {
            placeLiteContent(grid, previewSeed ^ (omtX * 31L + omtY), numDirs, anySidewalk);
        }
        if (mapExtraId != null && !mapExtraId.isEmpty()) {
            applyExtraStub(grid, mapExtraId, previewSeed);
        }
        if (orient.rot != 0) {
            grid = MapGridRotator.rotate(grid, orient.rot);
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
        // Fallback: neighbor scan (pre-polish generic road / bridge spans)
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
        if (count(roads) == 0 && omtId != null && isBridgeOmtId(omtId)) {
            // Bare bridge cell with no road neighbors yet: default to a straight NS span.
            roads[0] = true;
            roads[2] = true;
        }
        if (count(roads) == 0) {
            roads[0] = roads[1] = roads[2] = roads[3] = true;
        }
        return roads;
    }

    static boolean isBridgeOmtId(final String omtId) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        return n.equals("bridge")
            || n.startsWith("bridge_")
            || n.startsWith("bridgehead")
            || n.equals("test_bridge")
            || n.startsWith("test_bridge");
    }

    /**
     * BN {@code mapgen_road} rot/diag selection so paint only sees
     * {@code |}, {@code '-}, {@code -'-}, {@code -|-}, or NE diagonal.
     */
    private static Orientation canonicalize(final boolean[] roads, final int numDirs) {
        int rot = 0;
        boolean diag = false;
        switch (numDirs) {
            case 3:
                if (!roads[0]) {
                    rot = 2;
                } else if (!roads[1]) {
                    rot = 3;
                } else if (!roads[3]) {
                    rot = 1;
                }
                break;
            case 2:
                if (roads[1] && roads[3]) {
                    rot = 1;
                } else if (roads[1] && roads[2]) {
                    rot = 1;
                    diag = true;
                } else if (roads[2] && roads[3]) {
                    rot = 2;
                    diag = true;
                } else if (roads[3] && roads[0]) {
                    rot = 3;
                    diag = true;
                } else if (roads[0] && roads[1]) {
                    diag = true;
                }
                break;
            case 1:
                if (roads[1]) {
                    rot = 1;
                } else if (roads[2]) {
                    rot = 2;
                } else if (roads[3]) {
                    rot = 3;
                }
                break;
            default:
                break;
        }
        return new Orientation(rot, diag);
    }

    /** Sidewalks first (all arms), then pavement + fillets, then yellow dots — BN order. */
    private static void paintOrthogonalCanonical(
        final MapGrid grid,
        final boolean[] roads,
        final int[] curve,
        final boolean[] sidewalks,
        final int numDirs,
        final int mapRot
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
        }
        if (deadEndExt > 0 && sidewalks[2]) {
            fillRect(grid, SIDEWALK, 0, SEEY + deadEndExt, SIZE - 1, SEEY + deadEndExt + 4);
        }

        for (int dir = 0; dir < 4; dir++) {
            if (!roads[dir]) {
                continue;
            }
            fillRotatedRect(grid, PAVEMENT, 4, 0, SIZE - 5, SEEY - 1 + deadEndExt, dir);
            if (curve[dir] != 0) {
                for (int x = 1; x < 4; x++) {
                    for (int y = 0; y < x; y++) {
                        // BN: curvedir == -1 → x, else SIZE-1-x
                        final int tx = curve[dir] == -1 ? x : SIZE - 1 - x;
                        final int ty = y;
                        final int[] p = rotateCw(tx, ty, dir);
                        grid.setTerrain(p[0], p[1], PAVEMENT);
                    }
                }
            }
        }

        for (int dir = 0; dir < 4; dir++) {
            if (!roads[dir]) {
                continue;
            }
            final int maxY = (numDirs == 4 || (numDirs == 3 && dir == 0)) ? 4 : SEEY;
            for (int x = SEEX - 1; x <= SEEX; x++) {
                for (int y = 0; y < maxY; y++) {
                    if ((y + ((dir + mapRot) / 2 % 2)) % 4 != 0) {
                        final int[] p = rotateCw(x, y, dir);
                        if (PAVEMENT.equals(grid.get(p[0], p[1]).getTerrainId())) {
                            grid.setTerrain(p[0], p[1], PAVEMENT_Y);
                        }
                    }
                }
            }
        }
    }

    /** Always paints the NE diagonal; caller rotates the grid afterward. */
    private static void paintDiagonalCanonical(
        final MapGrid grid,
        final int[] curve,
        final boolean[] sidewalks
    ) {
        // BN: sidewalk if NE/SE/SW neighbor (indices 4/5/6) has sidewalk after rot.
        if (sidewalks[4] || sidewalks[5] || sidewalks[6]) {
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
                if (x > y
                    && ((x > 3 && y < SIZE - 4)
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

    private static final int[][] NESW8_DELTA = {
        { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 },
        { 1, -1 }, { 1, 1 }, { -1, 1 }, { -1, -1 }
    };

    private static boolean[] sidewalkNeighbors(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final OvermapTerrainRegistry registry
    ) {
        final boolean[] out = new boolean[8];
        for (int i = 0; i < 8; i++) {
            final int x = omtX + NESW8_DELTA[i][0];
            final int y = omtY + NESW8_DELTA[i][1];
            out[i] = hasSidewalk(overmap, x, y, registry);
        }
        // City streets leave field/forest holes (~25% BUILDINGCHANCE skips). BN only puts
        // sidewalks next to SIDEWALK OMTs, so those holes break the strip. Promote natural
        // clearables that already abut an urban SIDEWALK lot so the street edge stays continuous.
        for (int i = 0; i < 8; i++) {
            if (out[i]) {
                continue;
            }
            final int x = omtX + NESW8_DELTA[i][0];
            final int y = omtY + NESW8_DELTA[i][1];
            if (!inBounds(overmap, x, y)) {
                continue;
            }
            final String id = overmap.getOmtId(x, y);
            if (isNaturalUrbanHole(id) && abutsSidewalkLot(overmap, x, y, registry)) {
                out[i] = true;
            }
        }
        return out;
    }

    private static boolean hasSidewalk(
        final OvermapGrid overmap,
        final int x,
        final int y,
        final OvermapTerrainRegistry registry
    ) {
        if (!inBounds(overmap, x, y)) {
            return false;
        }
        final String id = overmap.getOmtId(x, y);
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (hasSidewalkFlag(registry, id)) {
            return true;
        }
        // Rotatable lots may only have SIDEWALK on the unrotated base id.
        final String base = stripFacingSuffix(id);
        if (!base.equals(id) && hasSidewalkFlag(registry, base)) {
            return true;
        }
        final String n = id.toLowerCase(Locale.ROOT);
        return n.contains("house")
            || n.contains("shop")
            || n.contains("store")
            || n.contains("apartment")
            || n.contains("office")
            || n.contains("building")
            || n.contains("sidewalk")
            || n.contains("parking")
            || n.contains("restaurant")
            || n.contains("library")
            || n.contains("school")
            || n.contains("clinic")
            || n.contains("hospital")
            || n.contains("hotel")
            || n.contains("bank")
            || n.contains("gym")
            || n.contains("mall");
    }

    private static boolean inBounds(final OvermapGrid overmap, final int x, final int y) {
        return overmap != null && x >= 0 && y >= 0 && x < overmap.width() && y < overmap.height();
    }

    /** Unbuilt city flank leftover (field / forest / swamp), not roads or placed lots. */
    private static boolean isNaturalUrbanHole(final String omtId) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        if (n.startsWith("forest_trail") || n.startsWith("test_forest_trail") || n.startsWith("trailhead")) {
            return false;
        }
        if (n.contains("road") || n.contains("bridge") || n.contains("highway")) {
            return false;
        }
        return n.equals("field")
            || n.equals("test_field")
            || n.equals("open_air")
            || n.contains("forest")
            || n.contains("swamp");
    }

    private static boolean abutsSidewalkLot(
        final OvermapGrid overmap,
        final int x,
        final int y,
        final OvermapTerrainRegistry registry
    ) {
        for (final int[] d : NESW8_DELTA) {
            final int nx = x + d[0];
            final int ny = y + d[1];
            if (!inBounds(overmap, nx, ny)) {
                continue;
            }
            final String id = overmap.getOmtId(nx, ny);
            if (id == null || id.isEmpty()) {
                continue;
            }
            if (hasSidewalkFlag(registry, id)) {
                return true;
            }
            final String base = stripFacingSuffix(id);
            if (!base.equals(id) && hasSidewalkFlag(registry, base)) {
                return true;
            }
            // Name heuristics for lots missing SIDEWALK on the base id.
            if (hasSidewalk(overmap, nx, ny, registry) && !isNaturalUrbanHole(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSidewalkFlag(final OvermapTerrainRegistry registry, final String id) {
        if (registry == null || id == null) {
            return false;
        }
        final OvermapTerrainDefinition def = registry.find(id).orElse(null);
        if (def == null) {
            return false;
        }
        for (final String flag : def.getFlags()) {
            if ("SIDEWALK".equals(flag) || "HAS_SIDEWALK".equals(flag)) {
                return true;
            }
        }
        return false;
    }

    private static String stripFacingSuffix(final String id) {
        final String lower = id.toLowerCase(Locale.ROOT);
        final String[] suffixes = {
            "_north_east", "_north_west", "_south_east", "_south_west",
            "_north", "_east", "_south", "_west"
        };
        for (final String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                return id.substring(0, id.length() - suffix.length());
            }
        }
        return id;
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
        return RoadConnectionPolisher.isRoadFamily(overmap.getOmtId(x, y), connections)
            || isBridgeOmtId(overmap.getOmtId(x, y));
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

    private static void fillRect(
        final MapGrid grid,
        final String terrain,
        final int x1,
        final int y1,
        final int x2,
        final int y2
    ) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
                    grid.setTerrain(x, y, terrain);
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

    /**
     * BN {@code nesw_array_rotate}: cyclic left for length 4; special 45° shuffle for
     * length 8 ({@code N E S W NE SE SW NW}).
     */
    private static void neswArrayRotate(final boolean[] array, final int dist) {
        if (array == null || array.length == 0 || dist == 0) {
            return;
        }
        int steps = Math.floorMod(dist, array.length == 8 ? 8 : 4);
        if (array.length == 4) {
            while (steps-- > 0) {
                final boolean temp = array[0];
                array[0] = array[1];
                array[1] = array[2];
                array[2] = array[3];
                array[3] = temp;
            }
            return;
        }
        if (array.length != 8) {
            return;
        }
        while (steps-- > 0) {
            // N E S W NE SE SW NW — one step = 45° CCW on the compass
            final boolean temp = array[0];
            array[0] = array[4];
            array[4] = array[1];
            array[1] = array[5];
            array[5] = array[2];
            array[2] = array[6];
            array[6] = array[3];
            array[3] = array[7];
            array[7] = temp;
        }
    }

    private static void neswArrayRotate(final int[] array, final int dist) {
        if (array == null || array.length != 4 || dist == 0) {
            return;
        }
        int steps = Math.floorMod(dist, 4);
        while (steps-- > 0) {
            final int temp = array[0];
            array[0] = array[1];
            array[1] = array[2];
            array[2] = array[3];
            array[3] = temp;
        }
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

    private static final class Orientation {
        final int rot;
        final boolean diag;

        Orientation(final int rot, final boolean diag) {
            this.rot = rot;
            this.diag = diag;
        }
    }
}
