package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** Connects placed city/special sites with road OMT ids (W5 v1). */
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
        final String preferredRoad = connection.resolveTerrainId();
        String roadId = OrthogonalPathCarver.resolveTerrainId(preferredRoad, "road", registry);
        if (registry != null && !registry.contains(roadId)) {
            roadId = OrthogonalPathCarver.resolveTerrainId("test_road", preferredRoad, registry);
        }
        if (registry != null && !registry.contains(roadId)) {
            addWarning(warnings, "road terrain '" + roadId + "' not in registry; skipping roads");
            return 0;
        }

        final Set<String> overwritable = OrthogonalPathCarver.terrainOverwritableIds(
            options,
            registry,
            roadId,
            options.getRiverCenterId(),
            options.getRiverBankId()
        );

        int painted = 0;
        for (int i = 0; i < sites.size() - 1; i++) {
            final int[] from = sites.get(i);
            final int[] to = sites.get(i + 1);
            final List<int[]> path = OrthogonalPathCarver.buildPath(from[0], from[1], to[0], to[1], rng);
            painted += OrthogonalPathCarver.paintPath(grid, path, roadId, overwritable);
        }
        return painted;
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
