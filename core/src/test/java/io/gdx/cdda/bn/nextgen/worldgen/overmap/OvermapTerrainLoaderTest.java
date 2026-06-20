package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapTerrainLoaderTest {

    @Test
    void loadsFixtureOvermapTerrainIds() throws Exception {
        final OvermapTerrainLoadResult result = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );

        assertTrue(result.getRegistry().size() >= 5);
        assertTrue(result.getRegistry().contains("open_air"));
        assertTrue(result.getRegistry().contains("test_room"));
        assertTrue(result.getRegistry().contains("test_duplex_ground_north"));

        final OvermapTerrainDefinition room = result.getRegistry().find("test_room").orElseThrow();
        assertEquals("R", room.getSymbol());
        assertEquals(1, room.jsonMapgenRefCount());
        assertEquals("test_room", room.getMapgenRefs().get(0).getOmTerrain());
    }
}
