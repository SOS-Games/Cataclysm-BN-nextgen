package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Shared orthogonal connection carving for roads and underground networks (W17f). */
final class ConnectionPathGenerator {

    @FunctionalInterface
    interface CellPaintFilter {
        boolean canPaint(int x, int y, String existingOmtId);
    }

    private ConnectionPathGenerator() {}

    static int connectUrbanSites(
        final OvermapGrid grid,
        final List<UrbanSite> urbanSites,
        final String connectionId,
        final String bnFallbackTerrainId,
        final String testFallbackTerrainId,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings,
        final CellPaintFilter cellFilter
    ) {
        if (grid == null || options == null || urbanSites == null || urbanSites.size() < 2) {
            return 0;
        }
        final List<int[]> centers = new ArrayList<>(urbanSites.size());
        for (final UrbanSite site : urbanSites) {
            centers.add(site.center());
        }
        centers.sort(Comparator.comparingInt((final int[] c) -> c[0]).thenComparingInt(c -> c[1]));
        return paintEndpointPairs(
            grid,
            minimumSpanningTreePairs(centers),
            connectionId,
            bnFallbackTerrainId,
            testFallbackTerrainId,
            connections,
            options,
            registry,
            rng,
            warnings,
            cellFilter
        );
    }

    static int paintEndpointPairs(
        final OvermapGrid grid,
        final List<int[]> endpointPairs,
        final String connectionId,
        final String bnFallbackTerrainId,
        final String testFallbackTerrainId,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings,
        final CellPaintFilter cellFilter
    ) {
        if (endpointPairs == null || endpointPairs.size() < 2) {
            return 0;
        }
        final OvermapConnectionDefinition connection = resolveConnection(
            connections,
            connectionId,
            warnings
        );
        if (connection == null) {
            return 0;
        }
        final String terrainId = OrthogonalPathCarver.resolveTerrainId(
            OrthogonalPathCarver.resolveTerrainId(connection.resolveTerrainId(), bnFallbackTerrainId, registry),
            testFallbackTerrainId,
            registry
        );
        if (registry != null && !registry.contains(terrainId) && !hasAnySubtype(registry, connection)) {
            addWarning(warnings, connectionId + " terrain '" + terrainId + "' not in registry; skipping carve");
            return 0;
        }

        final Set<String> overwritable = new HashSet<>(
            OrthogonalPathCarver.terrainOverwritableIds(
                options,
                registry,
                terrainId,
                options.getRiverCenterId(),
                options.getRiverBankId()
            )
        );
        for (final String subtype : connection.getSubtypeTerrains()) {
            if (subtype != null && !subtype.isEmpty()) {
                overwritable.add(subtype);
            }
        }
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
                    if (cellFilter != null && !cellFilter.canPaint(x, y, existing)) {
                        return null;
                    }
                    final String picked = connection.pickTerrainForStep(fromX, fromY, x, y, existing, options);
                    return OrthogonalPathCarver.resolveTerrainId(picked, terrainId, registry);
                },
                overwritable
            );
        }
        return painted;
    }

    static List<int[]> minimumSpanningTreePairs(final List<int[]> centers) {
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

    static List<int[]> extremalPairs(final List<int[]> centers) {
        if (centers.size() < 2) {
            return Collections.emptyList();
        }
        int west = 0;
        int east = 0;
        int north = 0;
        int south = 0;
        for (int i = 1; i < centers.size(); i++) {
            if (centers.get(i)[0] < centers.get(west)[0]) {
                west = i;
            }
            if (centers.get(i)[0] > centers.get(east)[0]) {
                east = i;
            }
            if (centers.get(i)[1] < centers.get(north)[1]) {
                north = i;
            }
            if (centers.get(i)[1] > centers.get(south)[1]) {
                south = i;
            }
        }
        final List<int[]> pairs = new ArrayList<>(4);
        if (west != east) {
            pairs.add(centers.get(west));
            pairs.add(centers.get(east));
        }
        if (north != south) {
            pairs.add(centers.get(north));
            pairs.add(centers.get(south));
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

    static OvermapConnectionDefinition resolveConnection(
        final OvermapConnectionRegistry connections,
        final String connectionId,
        final List<String> warnings
    ) {
        if (connections != null && connectionId != null && !connectionId.isEmpty()) {
            final OvermapConnectionDefinition found = connections.find(connectionId).orElse(null);
            if (found != null) {
                return found;
            }
        }
        addWarning(warnings, "connection template '" + connectionId + "' not found; skipping carve");
        return null;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null && message != null && !message.isEmpty()) {
            warnings.add(message);
        }
    }
}
