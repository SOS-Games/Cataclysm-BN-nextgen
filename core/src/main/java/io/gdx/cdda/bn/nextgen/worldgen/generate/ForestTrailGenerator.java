package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.ForestTrailSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapForestSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Carves {@code forest_trail} OMT paths through contiguous forest blobs (W17e). */
public final class ForestTrailGenerator {

    private static final String FOREST_TRAIL_CONNECTION_ID = "forest_trail";
    private static final String TEST_FOREST_TRAIL_CONNECTION_ID = "test_forest_trail";

    private ForestTrailGenerator() {}

    public static int placeAll(
        final OvermapGrid grid,
        final RegionSettingsDefinition region,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || region == null || options == null) {
            return 0;
        }
        final ForestTrailSettings settings = region.getForestTrailSettings();
        if (!settings.isEnabled()) {
            return 0;
        }
        final OvermapConnectionDefinition connection = resolveConnection(connections, registry, warnings);
        if (connection == null) {
            return 0;
        }
        final String trailId = OrthogonalPathCarver.resolveTerrainId(
            connection.resolveTerrainId(),
            "forest_trail",
            registry
        );
        final String resolvedTrailId = OrthogonalPathCarver.resolveTerrainId(trailId, "test_forest_trail", registry);
        if (registry != null && !registry.contains(resolvedTrailId)) {
            addWarning(warnings, "forest trail terrain '" + resolvedTrailId + "' not in registry; skipping trails");
            return 0;
        }

        final Set<String> forestIds = forestTerrainIds(region, options);
        final Set<String> overwritable = new HashSet<>(forestIds);
        overwritable.add("field");
        overwritable.add("test_field");
        overwritable.add("open_air");
        overwritable.add("swamp");
        overwritable.add("test_swamp");
        overwritable.add(resolvedTrailId);
        final boolean[] visited = new boolean[grid.width() * grid.height()];
        int painted = 0;

        for (int y = 1; y < grid.height() - 1; y++) {
            for (int x = 1; x < grid.width() - 1; x++) {
                final int index = y * grid.width() + x;
                if (visited[index]) {
                    continue;
                }
                if (!isForestTerrain(grid.getOmtId(x, y), forestIds)) {
                    continue;
                }
                final List<int[]> forestPoints = floodFillForest(grid, x, y, forestIds, visited);
                if (forestPoints.size() < settings.getMinimumForestSize()) {
                    continue;
                }
                if (!oneIn(rng, settings.getChance())) {
                    continue;
                }
                painted += carveTrailInForest(
                    grid,
                    forestPoints,
                    settings,
                    connection,
                    resolvedTrailId,
                    overwritable,
                    forestIds,
                    options,
                    rng
                );
            }
        }

        painted += placeTrailheads(grid, settings, resolvedTrailId, registry, rng);
        return painted;
    }

    private static int carveTrailInForest(
        final OvermapGrid grid,
        final List<int[]> forestPoints,
        final ForestTrailSettings settings,
        final OvermapConnectionDefinition connection,
        final String trailId,
        final Set<String> overwritable,
        final Set<String> forestIds,
        final OvermapGenerateOptions options,
        final Random rng
    ) {
        int northmostY = Integer.MAX_VALUE;
        int southmostY = Integer.MIN_VALUE;
        int westmostX = Integer.MAX_VALUE;
        int eastmostX = Integer.MIN_VALUE;
        int[] northmost = forestPoints.get(0);
        int[] southmost = forestPoints.get(0);
        int[] westmost = forestPoints.get(0);
        int[] eastmost = forestPoints.get(0);

        for (final int[] point : forestPoints) {
            if (point[1] < northmostY) {
                northmostY = point[1];
                northmost = point;
            }
            if (point[1] > southmostY) {
                southmostY = point[1];
                southmost = point;
            }
            if (point[0] < westmostX) {
                westmostX = point[0];
                westmost = point;
            }
            if (point[0] > eastmostX) {
                eastmostX = point[0];
                eastmost = point;
            }
        }

        final int centerX = westmostX + (eastmostX - westmostX) / 2;
        final int centerY = northmostY + (southmostY - northmostY) / 2;
        final int[] actualCenter = closestPoint(forestPoints, centerX, centerY);

        int maxRandomPoints = settings.getRandomPointMin()
            + forestPoints.size() / settings.getRandomPointSizeScalar();
        maxRandomPoints = Math.min(maxRandomPoints, settings.getRandomPointMax());

        final List<int[]> chosen = new ArrayList<>();
        chosen.add(actualCenter);

        final List<int[]> shuffled = new ArrayList<>(forestPoints);
        Collections.shuffle(shuffled, rng);
        int randomCount = 0;
        for (final int[] point : shuffled) {
            if (randomCount >= maxRandomPoints) {
                break;
            }
            randomCount++;
            chosen.add(point);
        }

        if (oneIn(rng, settings.getBorderPointChance())) {
            chosen.add(northmost);
        }
        if (oneIn(rng, settings.getBorderPointChance())) {
            chosen.add(southmost);
        }
        if (oneIn(rng, settings.getBorderPointChance())) {
            chosen.add(westmost);
        }
        if (oneIn(rng, settings.getBorderPointChance())) {
            chosen.add(eastmost);
        }

        final List<int[]> uniqueChosen = dedupePoints(chosen);
        if (uniqueChosen.size() < 2) {
            return 0;
        }

        uniqueChosen.sort(Comparator.comparingInt((final int[] p) -> p[0]).thenComparingInt(p -> p[1]));
        final List<int[]> pairs = minimumSpanningTreePairs(uniqueChosen);
        int painted = 0;
        for (int i = 0; i < pairs.size(); i += 2) {
            final int[] from = pairs.get(i);
            final int[] to = pairs.get(i + 1);
            final List<int[]> path = OrthogonalPathCarver.buildPath(from[0], from[1], to[0], to[1], rng);
            painted += OrthogonalPathCarver.paintDirectionalPath(
                grid,
                path,
                (fromX, fromY, x, y, existing) -> {
                    // BN forest_trail also crosses forest_edge (field) and swamp between blobs.
                    if (!isTrailableTerrain(existing, forestIds, trailId)) {
                        return null;
                    }
                    final String picked = connection.pickTerrainForStep(fromX, fromY, x, y, existing, options);
                    return OrthogonalPathCarver.resolveTerrainId(picked, trailId, null);
                },
                overwritable
            );
        }
        return painted;
    }

    private static int placeTrailheads(
        final OvermapGrid grid,
        final ForestTrailSettings settings,
        final String trailId,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        if (grid == null || settings.getTrailheads().isEmpty()) {
            return 0;
        }
        final String trailheadId = pickTrailhead(settings.getTrailheads(), registry, rng);
        if (trailheadId == null || trailheadId.isEmpty()) {
            return 0;
        }
        if (registry != null && !registry.contains(trailheadId)) {
            return 0;
        }

        int placed = 0;
        for (int y = 2; y < grid.height() - 2; y++) {
            for (int x = 2; x < grid.width() - 2; x++) {
                if (!isTrailTerrain(grid.getOmtId(x, y), trailId)) {
                    continue;
                }
                final int[] neighbor = singleTrailNeighbor(grid, x, y, trailId);
                if (neighbor == null) {
                    continue;
                }
                if (!oneIn(rng, settings.getTrailheadChance())) {
                    continue;
                }
                final int awayX = x + (x - neighbor[0]);
                final int awayY = y + (y - neighbor[1]);
                if (awayX < 0 || awayY < 0 || awayX >= grid.width() || awayY >= grid.height()) {
                    continue;
                }
                if (!isForestTerrain(grid.getOmtId(awayX, awayY), Collections.emptySet())
                    && !isFieldLike(grid.getOmtId(awayX, awayY))) {
                    continue;
                }
                if (!isNearRoad(grid, awayX, awayY, settings.getTrailheadRoadDistance())) {
                    continue;
                }
                if (grid.getOmtId(awayX, awayY).equals(trailheadId)) {
                    continue;
                }
                grid.setOmtId(awayX, awayY, trailheadId);
                placed++;
            }
        }
        return placed;
    }

    private static boolean isNearRoad(final OvermapGrid grid, final int x, final int y, final int distance) {
        final ArrayDeque<int[]> queue = new ArrayDeque<>();
        final Set<Long> seen = new HashSet<>();
        queue.add(new int[] { x, y, 0 });
        seen.add(pack(x, y));

        while (!queue.isEmpty()) {
            final int[] current = queue.removeFirst();
            final int cx = current[0];
            final int cy = current[1];
            final int depth = current[2];
            if (isRoadTerrain(grid.getOmtId(cx, cy))) {
                return true;
            }
            if (depth >= distance) {
                continue;
            }
            for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                final int nx = cx + step[0];
                final int ny = cy + step[1];
                if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                    continue;
                }
                final long key = pack(nx, ny);
                if (seen.add(key)) {
                    queue.add(new int[] { nx, ny, depth + 1 });
                }
            }
        }
        return false;
    }

    private static int[] singleTrailNeighbor(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String trailId
    ) {
        int[] found = null;
        for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            final int nx = x + step[0];
            final int ny = y + step[1];
            if (!isTrailTerrain(grid.getOmtId(nx, ny), trailId)) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = new int[] { nx, ny };
        }
        return found;
    }

    private static List<int[]> floodFillForest(
        final OvermapGrid grid,
        final int startX,
        final int startY,
        final Set<String> forestIds,
        final boolean[] visited
    ) {
        final List<int[]> points = new ArrayList<>();
        final ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { startX, startY });
        visited[startY * grid.width() + startX] = true;

        while (!queue.isEmpty()) {
            final int[] current = queue.removeFirst();
            points.add(current);
            for (final int[] step : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                final int nx = current[0] + step[0];
                final int ny = current[1] + step[1];
                if (nx <= 0 || ny <= 0 || nx >= grid.width() - 1 || ny >= grid.height() - 1) {
                    continue;
                }
                final int index = ny * grid.width() + nx;
                if (visited[index]) {
                    continue;
                }
                if (!isForestTerrain(grid.getOmtId(nx, ny), forestIds)) {
                    continue;
                }
                visited[index] = true;
                queue.add(new int[] { nx, ny });
            }
        }
        return points;
    }

    private static Set<String> forestTerrainIds(
        final RegionSettingsDefinition region,
        final OvermapGenerateOptions options
    ) {
        final Set<String> ids = new LinkedHashSet<>();
        final OvermapForestSettings forest = region.getForestSettings();
        ids.add(forest.getForestOter());
        ids.add(forest.getForestThickOter());
        if (options.getForestId() != null && !options.getForestId().isEmpty()) {
            ids.add(options.getForestId());
        }
        ids.add("forest");
        ids.add("forest_thick");
        ids.add("forest_water");
        ids.add("test_forest_thick");
        return ids;
    }

    private static boolean isForestTerrain(final String omtId, final Set<String> forestIds) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        if (omtId.startsWith("forest_trail") || omtId.startsWith("trailhead")) {
            return false;
        }
        if (forestIds.contains(omtId)) {
            return true;
        }
        return omtId.startsWith("forest");
    }

    private static boolean isTrailTerrain(final String omtId, final String trailId) {
        if (omtId == null) {
            return false;
        }
        if (omtId.equals(trailId)) {
            return true;
        }
        return omtId.startsWith("forest_trail") || omtId.startsWith("test_forest_trail");
    }

    /** Forest, existing trail, field/edge, or swamp — BN forest_trail connection locations. */
    private static boolean isTrailableTerrain(
        final String omtId,
        final Set<String> forestIds,
        final String trailId
    ) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        if (isTrailTerrain(omtId, trailId) || isForestTerrain(omtId, forestIds)) {
            return true;
        }
        if (isFieldLike(omtId)) {
            return true;
        }
        final String n = omtId.toLowerCase(java.util.Locale.ROOT);
        return n.contains("swamp") || n.equals("forest_edge");
    }

    private static boolean isRoadTerrain(final String omtId) {
        return omtId != null && (omtId.contains("road") || "test_bridge".equals(omtId));
    }

    private static boolean isFieldLike(final String omtId) {
        return omtId != null
            && (omtId.equals("field") || omtId.equals("test_field") || omtId.equals("open_air"));
    }

    private static int[] closestPoint(final List<int[]> points, final int x, final int y) {
        int[] best = points.get(0);
        int bestDistance = Integer.MAX_VALUE;
        for (final int[] point : points) {
            final int distance = Math.abs(point[0] - x) + Math.abs(point[1] - y);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = point;
            }
        }
        return best;
    }

    private static List<int[]> dedupePoints(final List<int[]> points) {
        final List<int[]> unique = new ArrayList<>();
        final Set<Long> seen = new HashSet<>();
        for (final int[] point : points) {
            final long key = pack(point[0], point[1]);
            if (seen.add(key)) {
                unique.add(point);
            }
        }
        return unique;
    }

    private static List<int[]> minimumSpanningTreePairs(final List<int[]> centers) {
        if (centers.size() < 2) {
            return Collections.emptyList();
        }
        final boolean[] inTree = new boolean[centers.size()];
        inTree[0] = true;
        final List<int[]> pairs = new ArrayList<>();
        for (int added = 1; added < centers.size(); added++) {
            int bestFrom = -1;
            int bestTo = -1;
            int bestDistance = Integer.MAX_VALUE;
            for (int from = 0; from < centers.size(); from++) {
                if (!inTree[from]) {
                    continue;
                }
                for (int to = 0; to < centers.size(); to++) {
                    if (inTree[to]) {
                        continue;
                    }
                    final int distance = manhattanDistance(centers.get(from), centers.get(to));
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestFrom = from;
                        bestTo = to;
                    }
                }
            }
            if (bestFrom < 0 || bestTo < 0) {
                break;
            }
            pairs.add(centers.get(bestFrom));
            pairs.add(centers.get(bestTo));
            inTree[bestTo] = true;
        }
        return pairs;
    }

    private static int manhattanDistance(final int[] from, final int[] to) {
        return Math.abs(from[0] - to[0]) + Math.abs(from[1] - to[1]);
    }

    private static String pickTrailhead(
        final Map<String, Integer> weights,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        int total = 0;
        for (final Map.Entry<String, Integer> entry : weights.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            if (registry != null && !registry.contains(entry.getKey())) {
                continue;
            }
            total += Math.max(1, entry.getValue());
        }
        if (total <= 0) {
            return null;
        }
        int roll = rng.nextInt(total);
        for (final Map.Entry<String, Integer> entry : weights.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            if (registry != null && !registry.contains(entry.getKey())) {
                continue;
            }
            roll -= Math.max(1, entry.getValue());
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }

    private static OvermapConnectionDefinition resolveConnection(
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        if (connections != null) {
            final OvermapConnectionDefinition found = connections.find(FOREST_TRAIL_CONNECTION_ID).orElse(null);
            if (found != null) {
                return found;
            }
            final OvermapConnectionDefinition test = connections.find(TEST_FOREST_TRAIL_CONNECTION_ID).orElse(null);
            if (test != null) {
                return test;
            }
        }
        addWarning(warnings, "forest_trail connection not found; skipping forest trails");
        return null;
    }

    private static boolean oneIn(final Random rng, final int chance) {
        if (chance <= 0) {
            return false;
        }
        if (chance == 1) {
            return true;
        }
        return rng.nextInt(chance) == 0;
    }

    private static long pack(final int x, final int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null && message != null && !message.isEmpty()) {
            warnings.add(message);
        }
    }
}
