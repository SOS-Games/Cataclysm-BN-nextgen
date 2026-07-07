package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Railroad lines between urban centers via {@code local_railroad} (W17f lite). */
public final class RailGenerator {

    private static final String RAIL_CONNECTION_ID = "local_railroad";
    private static final String TEST_RAIL_CONNECTION_ID = "test_local_railroad";

    private RailGenerator() {}

    public static int connectCities(
        final OvermapGrid grid,
        final List<UrbanSite> urbanSites,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || urbanSites == null || urbanSites.size() < 2) {
            return 0;
        }
        final List<int[]> centers = new ArrayList<>(urbanSites.size());
        for (final UrbanSite site : urbanSites) {
            centers.add(site.center());
        }
        centers.sort(Comparator.comparingInt((final int[] c) -> c[0]).thenComparingInt(c -> c[1]));
        final String connectionId = resolveConnectionId(connections);
        return ConnectionPathGenerator.paintEndpointPairs(
            grid,
            ConnectionPathGenerator.extremalPairs(centers),
            connectionId,
            "railroad",
            "test_railroad",
            connections,
            options,
            registry,
            rng,
            warnings,
            null
        );
    }

    private static String resolveConnectionId(final OvermapConnectionRegistry connections) {
        if (connections != null && connections.contains(RAIL_CONNECTION_ID)) {
            return RAIL_CONNECTION_ID;
        }
        if (connections != null && connections.contains(TEST_RAIL_CONNECTION_ID)) {
            return TEST_RAIL_CONNECTION_ID;
        }
        return RAIL_CONNECTION_ID;
    }
}
