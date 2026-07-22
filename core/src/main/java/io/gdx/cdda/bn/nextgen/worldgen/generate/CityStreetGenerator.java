package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * BN {@code build_city_street} / {@code lay_out_street} road-only growth (C1) with manhole
 * collection (C4).
 */
public final class CityStreetGenerator {

    private static final String LOCAL_ROAD_CONNECTION_ID = "local_road";
    private static final String TEST_LOCAL_ROAD_CONNECTION_ID = "test_local_road";
    private static final int MAX_RECURSION = 48;
    private static final int[][] DELTA = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } }; // N E S W

    private CityStreetGenerator() {}

    public static final class StreetNode {
        public final int x;
        public final int y;
        /** Direction of travel into this node (0=N..3=W), or -1 for center. */
        public final int travelDir;

        public StreetNode(final int x, final int y, final int travelDir) {
            this.x = x;
            this.y = y;
            this.travelDir = travelDir;
        }
    }

    public static final class GrowResult {
        private final int roadCells;
        private final List<StreetNode> streetNodes;
        private final List<int[]> sewerPoints;

        public GrowResult(
            final int roadCells,
            final List<StreetNode> streetNodes,
            final List<int[]> sewerPoints
        ) {
            this.roadCells = Math.max(0, roadCells);
            this.streetNodes = streetNodes == null ? List.of() : List.copyOf(streetNodes);
            this.sewerPoints = sewerPoints == null ? List.of() : List.copyOf(sewerPoints);
        }

        public int getRoadCells() {
            return roadCells;
        }

        public List<StreetNode> getStreetNodes() {
            return streetNodes;
        }

        public List<int[]> getSewerPoints() {
            return sewerPoints;
        }
    }

    public static GrowResult growCity(
        final OvermapGrid grid,
        final UrbanSite site,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        return growCity(grid, site, connections, options, registry, null, rng, warnings);
    }

    public static GrowResult growCity(
        final OvermapGrid grid,
        final UrbanSite site,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition region,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || site == null || options == null || !options.isRoadsEnabled() || rng == null) {
            return new GrowResult(0, List.of(), List.of());
        }
        final OvermapConnectionDefinition connection = resolveLocalRoad(connections, warnings);
        if (connection == null) {
            return new GrowResult(0, List.of(), List.of());
        }
        final String roadId = OrthogonalPathCarver.resolveTerrainId(
            connection.resolveTerrainId(),
            "test_road",
            registry
        );
        final String manholeId = resolveManholeId(roadId, registry);
        final Set<String> overwritable = buildOverwritable(options, registry, roadId, connection, region);

        final int[] seed = findClearableSeed(grid, site, overwritable, options);
        final int cx = seed[0];
        final int cy = seed[1];
        int painted = 0;
        if (UrbanTerrainClearables.isPaveable(grid.getOmtId(cx, cy), overwritable, options)) {
            grid.setOmtId(cx, cy, manholeId);
            painted++;
        } else {
            return new GrowResult(0, List.of(), List.of());
        }

        final List<StreetNode> nodes = new ArrayList<>();
        nodes.add(new StreetNode(cx, cy, -1));
        final List<int[]> sewers = new ArrayList<>();
        sewers.add(new int[] { cx, cy });

        final int startDir = rng.nextInt(4);
        int dir = startDir;
        do {
            painted += buildCityStreet(
                grid,
                connection,
                roadId,
                manholeId,
                overwritable,
                options,
                registry,
                rng,
                cx,
                cy,
                site.getRadius(),
                dir,
                2,
                0,
                nodes,
                sewers
            );
            dir = turnRight(dir);
        } while (dir != startDir);

        painted += connectSewers(grid, cx, cy, sewers, options, registry, rng);

        // Keep the center seed as manhole after arm paints / four-way rolls.
        if (UrbanTerrainClearables.isPaveable(grid.getOmtId(cx, cy), overwritable, options)
            || UrbanTerrainClearables.isRoadFamily(grid.getOmtId(cx, cy))) {
            grid.setOmtId(cx, cy, manholeId);
        }

        final String fillId = OrthogonalPathCarver.resolveTerrainId(
            options.getFieldId(),
            "field",
            registry
        );
        ParallelRoadLaneDissolver.dissolve(grid, fillId);
        RoadGapFiller.fill(grid, roadId, options);
        ParallelRoadLaneDissolver.dissolve(grid, fillId);

        return new GrowResult(painted, nodes, sewers);
    }

    private static int buildCityStreet(
        final OvermapGrid grid,
        final OvermapConnectionDefinition connection,
        final String roadId,
        final String manholeId,
        final Set<String> overwritable,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final int startX,
        final int startY,
        final int cs,
        final int dir,
        final int blockWidth,
        final int depth,
        final List<StreetNode> nodes,
        final List<int[]> sewers
    ) {
        if (depth > MAX_RECURSION || cs < 1 || dir < 0 || dir > 3) {
            return 0;
        }
        final List<int[]> path = layOutStreet(
            grid, connection, options, overwritable, startX, startY, dir, cs + 1
        );
        if (path.isEmpty()) {
            return 0;
        }
        int painted = paintPath(grid, path, connection, roadId, overwritable, options, registry);
        for (final int[] cell : path) {
            nodes.add(new StreetNode(cell[0], cell[1], dir));
        }

        int c = cs;
        int croad = cs;
        // Alternate thick/thin blocks like BN (2 ↔ 3–5).
        final int newWidth = blockWidth == 2 ? 3 + rng.nextInt(3) : 2;
        for (int i = 0; i < path.size(); i++) {
            c--;
            final int x = path.get(i)[0];
            final int y = path.get(i)[1];
            if (c >= 2 && c < croad - blockWidth) {
                croad = c;
                // BN: left/right = cs - rng(1,3); only bump exact nubs of length 1.
                int left = cs - (1 + rng.nextInt(3));
                int right = cs - (1 + rng.nextInt(3));
                if (left == 1) {
                    left++;
                }
                if (right == 1) {
                    right++;
                }
                painted += buildCityStreet(
                    grid, connection, roadId, manholeId, overwritable, options, registry, rng,
                    x, y, left, turnLeft(dir), newWidth, depth + 1, nodes, sewers
                );
                painted += buildCityStreet(
                    grid, connection, roadId, manholeId, overwritable, options, registry, rng,
                    x, y, right, turnRight(dir), newWidth, depth + 1, nodes, sewers
                );
                if (rng.nextInt(8) == 0 && isFourWayCandidate(grid, x, y)) {
                    grid.setOmtId(x, y, manholeId);
                    sewers.add(new int[] { x, y });
                }
            }
        }

        // BN: end turn when the arm walked its full budget (c reaches 0). Our path excludes the
        // source cell and can be cs+1 long, so c may land at -1 — treat c <= 0 as "full arm".
        int nextCs = cs - (1 + rng.nextInt(3));
        if (nextCs >= 2 && c <= 0) {
            final int[] last = path.get(path.size() - 1);
            final int rnd = rng.nextBoolean() ? turnLeft(dir) : turnRight(dir);
            // BN uses default block_width=2 for the first edge spur (little neighborhoods).
            painted += buildCityStreet(
                grid, connection, roadId, manholeId, overwritable, options, registry, rng,
                last[0], last[1], nextCs, rnd, 2, depth + 1, nodes, sewers
            );
            if (rng.nextInt(5) == 0) {
                painted += buildCityStreet(
                    grid, connection, roadId, manholeId, overwritable, options, registry, rng,
                    last[0], last[1], nextCs, opposite(rnd), newWidth, depth + 1, nodes, sewers
                );
            }
        }
        return painted;
    }

    static List<int[]> layOutStreet(
        final OvermapGrid grid,
        final OvermapConnectionDefinition connection,
        final OvermapGenerateOptions options,
        final Set<String> overwritable,
        final int sourceX,
        final int sourceY,
        final int dir,
        final int len
    ) {
        int planned = Math.max(1, len);
        final int ex = sourceX + DELTA[dir][0] * (planned + 1);
        final int ey = sourceY + DELTA[dir][1] * (planned + 1);
        if (inboundsMargin(grid, ex, ey) && UrbanTerrainClearables.isRoadFamily(grid.getOmtId(ex, ey))) {
            planned++;
        }
        final List<int[]> path = new ArrayList<>();
        // Skip the source cell (already manhole / prior street); walk outward.
        int actual = 1;
        while (actual <= planned) {
            final int x = sourceX + DELTA[dir][0] * actual;
            final int y = sourceY + DELTA[dir][1] * actual;
            if (!inboundsMargin(grid, x, y)) {
                break;
            }
            final String ter = grid.getOmtId(x, y);
            if (UrbanTerrainClearables.isWaterBody(ter, options)
                || !canPlaceRoadOn(ter, connection, options, overwritable)) {
                break;
            }
            // BN stops at >=3 nearby road cells (excl. forward/back). That still allows a
            // parallel street to start beside a partial corridor (only 1–2 hits), then fill in
            // a second adjacent EW/NS lane — double-wide roads with a grass median. Also refuse
            // when either immediate perpendicular neighbor is already a road.
            if (countNearbyRoads(grid, x, y, dir) >= 3 || hasPerpendicularRoad(grid, x, y, dir)) {
                break;
            }
            path.add(new int[] { x, y });
            if (UrbanTerrainClearables.isRoadFamily(ter)) {
                break;
            }
            actual++;
        }
        return path;
    }

    /** True when a road lies on the immediate left or right of travel (parallel adjacency). */
    private static boolean hasPerpendicularRoad(
        final OvermapGrid grid,
        final int x,
        final int y,
        final int dir
    ) {
        final int left = turnLeft(dir);
        final int right = turnRight(dir);
        return isRoadAt(grid, x + DELTA[left][0], y + DELTA[left][1])
            || isRoadAt(grid, x + DELTA[right][0], y + DELTA[right][1]);
    }

    private static boolean isRoadAt(final OvermapGrid grid, final int x, final int y) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return UrbanTerrainClearables.isRoadFamily(grid.getOmtId(x, y));
    }

    private static int paintPath(
        final OvermapGrid grid,
        final List<int[]> path,
        final OvermapConnectionDefinition connection,
        final String roadId,
        final Set<String> overwritable,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        return OrthogonalPathCarver.paintDirectionalPath(
            grid,
            path,
            (fromX, fromY, x, y, existing) -> {
                if (UrbanTerrainClearables.isWaterBody(existing, options)) {
                    return null;
                }
                if (!UrbanTerrainClearables.isPaveable(existing, overwritable, options)) {
                    return null;
                }
                final String picked = connection.pickTerrainForStep(fromX, fromY, x, y, existing, options);
                return OrthogonalPathCarver.resolveTerrainId(picked, roadId, registry);
            },
            overwritable
        );
    }

    private static int connectSewers(
        final OvermapGrid grid,
        final int centerX,
        final int centerY,
        final List<int[]> sewers,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        // BN places these at z-1. Do not paint surface sewer OMTs (breaks sidewalks),
        // but still consume path RNG so city lot rolls stay seed-stable.
        if (sewers == null || rng == null) {
            return 0;
        }
        final Set<String> seen = new HashSet<>();
        for (final int[] point : sewers) {
            if (point[0] == centerX && point[1] == centerY) {
                continue;
            }
            final String key = point[0] + "," + point[1];
            if (!seen.add(key)) {
                continue;
            }
            OrthogonalPathCarver.buildPath(centerX, centerY, point[0], point[1], rng);
        }
        return 0;
    }

    private static boolean canPlaceRoadOn(
        final String ter,
        final OvermapConnectionDefinition connection,
        final OvermapGenerateOptions options,
        final Set<String> overwritable
    ) {
        return UrbanTerrainClearables.isPaveable(ter, overwritable, options);
    }

    private static boolean isFourWayCandidate(final OvermapGrid grid, final int x, final int y) {
        int arms = 0;
        for (final int[] d : DELTA) {
            final int nx = x + d[0];
            final int ny = y + d[1];
            if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                continue;
            }
            if (UrbanTerrainClearables.isRoadFamily(grid.getOmtId(nx, ny))) {
                arms++;
            }
        }
        return arms >= 3;
    }

    private static int countNearbyRoads(final OvermapGrid grid, final int x, final int y, final int dir) {
        int collisions = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                final int nx = x + i;
                final int ny = y + j;
                if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                    continue;
                }
                // Exclude forward and back along travel axis
                if (nx == x + DELTA[dir][0] && ny == y + DELTA[dir][1]) {
                    continue;
                }
                if (nx == x - DELTA[dir][0] && ny == y - DELTA[dir][1]) {
                    continue;
                }
                if (UrbanTerrainClearables.isRoadFamily(grid.getOmtId(nx, ny))) {
                    collisions++;
                }
            }
        }
        return collisions;
    }

    private static boolean inboundsMargin(final OvermapGrid grid, final int x, final int y) {
        return x >= 1 && y >= 1 && x < grid.width() - 1 && y < grid.height() - 1;
    }

    private static int[] findClearableSeed(
        final OvermapGrid grid,
        final UrbanSite site,
        final Set<String> overwritable,
        final OvermapGenerateOptions options
    ) {
        final int cx = site.getCenterX();
        final int cy = site.getCenterY();
        if (isSeedTerrainOk(grid.getOmtId(cx, cy), overwritable, options)) {
            return new int[] { cx, cy };
        }
        final int maxR = Math.max(2, site.getRadius());
        for (int r = 1; r <= maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) {
                        continue;
                    }
                    final int x = cx + dx;
                    final int y = cy + dy;
                    if (x < 1 || y < 1 || x >= grid.width() - 1 || y >= grid.height() - 1) {
                        continue;
                    }
                    if (isSeedTerrainOk(grid.getOmtId(x, y), overwritable, options)) {
                        return new int[] { x, y };
                    }
                }
            }
        }
        return new int[] { cx, cy };
    }

    private static boolean isSeedTerrainOk(
        final String ter,
        final Set<String> overwritable,
        final OvermapGenerateOptions options
    ) {
        if (ter == null) {
            return false;
        }
        if (UrbanTerrainClearables.isWaterBody(ter, options)) {
            return false;
        }
        return UrbanTerrainClearables.isPaveable(ter, overwritable, options);
    }

    private static int turnLeft(final int dir) {
        return (dir + 3) % 4;
    }

    private static int turnRight(final int dir) {
        return (dir + 1) % 4;
    }

    private static int opposite(final int dir) {
        return (dir + 2) % 4;
    }

    static int flankLeft(final int travelDir) {
        return turnLeft(travelDir);
    }

    static int flankRight(final int travelDir) {
        return turnRight(travelDir);
    }

    /** Direction a lot building should face (toward the street). */
    static int faceStreet(final int flankDir) {
        return opposite(flankDir);
    }

    static int[] displace(final int x, final int y, final int dir) {
        return new int[] { x + DELTA[dir][0], y + DELTA[dir][1] };
    }

    private static String resolveManholeId(final String roadId, final OvermapTerrainRegistry registry) {
        final String[] candidates = {
            roadId + "_nesw_manhole",
            "test_road_nesw_manhole",
            "road_nesw_manhole",
            roadId + "_nesw",
            "test_road_nesw",
            roadId
        };
        for (final String c : candidates) {
            if (registry == null || registry.contains(c)) {
                return c;
            }
        }
        return roadId;
    }

    private static Set<String> buildOverwritable(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final String roadId,
        final OvermapConnectionDefinition connection,
        final io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition region
    ) {
        final Set<String> ids = new HashSet<>(
            UrbanTerrainClearables.forCityGrowth(options, registry, region)
        );
        if (connection != null) {
            for (final String subtype : connection.getSubtypeTerrains()) {
                if (subtype != null && !subtype.isEmpty()) {
                    ids.add(subtype);
                }
            }
        }
        if (roadId != null && !roadId.isEmpty()) {
            ids.add(roadId);
        }
        ids.add("test_road_nesw_manhole");
        ids.add("test_road_nesw");
        ids.add("test_road_isolated");
        return ids;
    }

    private static OvermapConnectionDefinition resolveLocalRoad(
        final OvermapConnectionRegistry connections,
        final List<String> warnings
    ) {
        if (connections == null) {
            addWarning(warnings, "connection registry missing; skipping city streets");
            return null;
        }
        final OvermapConnectionDefinition local = connections.find(LOCAL_ROAD_CONNECTION_ID).orElse(null);
        if (local != null) {
            return local;
        }
        final OvermapConnectionDefinition test = connections.find(TEST_LOCAL_ROAD_CONNECTION_ID).orElse(null);
        if (test != null) {
            return test;
        }
        addWarning(warnings, "local_road connection not found; skipping city streets");
        return null;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
