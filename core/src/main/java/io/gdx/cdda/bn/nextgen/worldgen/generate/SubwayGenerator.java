package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;
import java.util.Random;

/** Inter-city subway tunnel carving via {@code subway_tunnel} (W17f lite). */
public final class SubwayGenerator {

    private static final String SUBWAY_CONNECTION_ID = "subway_tunnel";
    private static final String TEST_SUBWAY_CONNECTION_ID = "test_subway_tunnel";

    private SubwayGenerator() {}

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
        final String connectionId = resolveConnectionId(connections);
        return ConnectionPathGenerator.connectUrbanSites(
            grid,
            urbanSites,
            connectionId,
            "subway",
            "test_subway",
            connections,
            options,
            registry,
            rng,
            warnings,
            null
        );
    }

    private static String resolveConnectionId(final OvermapConnectionRegistry connections) {
        if (connections != null && connections.contains(SUBWAY_CONNECTION_ID)) {
            return SUBWAY_CONNECTION_ID;
        }
        if (connections != null && connections.contains(TEST_SUBWAY_CONNECTION_ID)) {
            return TEST_SUBWAY_CONNECTION_ID;
        }
        return SUBWAY_CONNECTION_ID;
    }
}
