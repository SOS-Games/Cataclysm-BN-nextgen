package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** Connects placed city/special sites with road OMT ids (W5 v1, W11c directional). */
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
        for (int i = 0; i < sites.size() - 1; i++) {
            final int[] from = sites.get(i);
            final int[] to = sites.get(i + 1);
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
