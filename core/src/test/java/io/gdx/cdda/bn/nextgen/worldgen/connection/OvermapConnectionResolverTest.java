package io.gdx.cdda.bn.nextgen.worldgen.connection;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapConnectionResolverTest {

    private OvermapConnectionRegistry registry;

    @BeforeEach
    void loadConnections() throws Exception {
        registry = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
    }

    @Test
    void resolvesRoadSubtypeToConnectionId() {
        assertEquals(
            "test_local_road",
            OvermapConnectionResolver.connectionIdForOmt("test_road_ns", registry).orElse("")
        );
    }

    @Test
    void ignoresNonRoadTerrain() {
        assertFalse(OvermapConnectionResolver.connectionIdForOmt("test_field", registry).isPresent());
    }

    @Test
    void mapsNeighborDirectionFromGrid() {
        final OvermapGrid overmap = new OvermapGrid(3, 3, "test_field");
        overmap.setOmtId(1, 0, "test_road_ns");

        final Map<String, String> connections = OvermapConnectionResolver.connectionsByDirection(
            overmap,
            1,
            1,
            registry
        );

        assertEquals("test_local_road", connections.get("north"));
        assertFalse(connections.containsKey("east"));
    }
}
