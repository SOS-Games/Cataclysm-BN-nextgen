package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Connects placed city/special sites with road OMT ids (W5 v1, W11c directional, W17c cities). */
public final class HighwayGenerator {

    private HighwayGenerator() {}

    public static int connectSites(
        final OvermapGrid grid,
        final List<int[]> sites,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || !options.isRoadsEnabled() || sites == null || sites.size() < 2) {
            return 0;
        }
        final List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < sites.size() - 1; i++) {
            pairs.add(sites.get(i));
            pairs.add(sites.get(i + 1));
        }
        return paintCenterPairs(grid, pairs, connections, options, registry, rng, warnings);
    }

    /** Inter-city highways using urban blob centers only (W17c). */
    public static int connectCities(
        final OvermapGrid grid,
        final List<UrbanSite> urbanSites,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || !options.isRoadsEnabled() || urbanSites == null || urbanSites.size() < 2) {
            return 0;
        }
        final List<int[]> centers = new ArrayList<>(urbanSites.size());
        for (final UrbanSite site : urbanSites) {
            centers.add(site.center());
        }
        centers.sort(Comparator.comparingInt((final int[] c) -> c[0]).thenComparingInt(c -> c[1]));
        return paintCenterPairs(
            grid,
            minimumSpanningTreePairs(centers),
            connections,
            options,
            registry,
            rng,
            warnings
        );
    }

    private static int paintCenterPairs(
        final OvermapGrid grid,
        final List<int[]> endpointPairs,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (endpointPairs == null || endpointPairs.size() < 2) {
            return 0;
        }
        final OvermapConnectionDefinition connection = resolveConnection(connections, options, warnings);
        if (connection == null) {
            return 0;
        }
        final String fallbackRoad = OrthogonalPathCarver.resolveTerrainId(
            connection.resolveTerrainId(),
            "road",
            registry
        );
        String roadId = OrthogonalPathCarver.resolveTerrainId(fallbackRoad, "test_road", registry);
        if (registry != null && !registry.contains(roadId) && !hasAnySubtype(registry, connection)) {
            addWarning(warnings, "road terrain '" + roadId + "' not in registry; skipping roads");
            return 0;
        }

        final java.util.HashSet<String> overwritable = new java.util.HashSet<>(
            OrthogonalPathCarver.terrainOverwritableIds(
                options,
                registry,
                roadId,
                options.getRiverCenterId(),
                options.getRiverBankId()
            )
        );
        if (connection.getBridgeTerrain() != null && !connection.getBridgeTerrain().isEmpty()) {
            overwritable.add(options.getRiverCenterId());
            overwritable.add("test_river");
        }

        int painted = 0;
        for (int i = 0; i < endpointPairs.size(); i += 2) {
            final int[] from = endpointPairs.get(i);
            final int[] to = endpointPairs.get(i + 1);
            final List<int[]> path = OrthogonalPathCarver.buildPath(from[0], from[1], to[0], to[1], rng);
            painted += OrthogonalPathCarver.paintDirectionalPath(
                grid,
                path,
                (fromX, fromY, x, y, existing) -> {
                    final String picked = connection.pickTerrainForStep(fromX, fromY, x, y, existing, options);
                    return OrthogonalPathCarver.resolveTerrainId(picked, roadId, registry);
                },
                overwritable
            );
        }
        return painted;
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

    private static boolean hasAnySubtype(
        final OvermapTerrainRegistry registry,
        final OvermapConnectionDefinition connection
    ) {
        if (registry == null) {
            return true;
        }
        for (final String terrainId : connection.getSubtypeTerrains()) {
            if (registry.contains(terrainId)) {
                return true;
            }
        }
        return false;
    }

    private static OvermapConnectionDefinition resolveConnection(
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final List<String> warnings
    ) {
        final String connectionId = options.getConnectionId();
        if (connections != null) {
            final OvermapConnectionDefinition found = connections.find(connectionId).orElse(null);
            if (found != null) {
                return found;
            }
        }
        addWarning(warnings, "connection template '" + connectionId + "' not found; skipping roads");
        return null;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
