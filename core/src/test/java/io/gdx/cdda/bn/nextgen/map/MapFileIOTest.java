package io.gdx.cdda.bn.nextgen.map;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapFileIOTest {

    @Test
    void roundTripPreservesTerrainIds() throws Exception {
        final MapGrid grid = new MapGrid(3, 3, "t_dirt");
        grid.setTerrain(0, 0, "t_grass");
        grid.setTerrain(1, 1, "t_concrete");
        grid.setTerrain(2, 2, "t_floor");

        final Path file = Files.createTempFile("nextgen-map-", ".json");
        try {
            MapFileIO.save(file, grid);
            final MapGrid loaded = MapFileIO.load(file);

            assertEquals(3, loaded.width());
            assertEquals(3, loaded.height());
            assertEquals("t_grass", loaded.get(0, 0).getTerrainId());
            assertEquals("t_concrete", loaded.get(1, 1).getTerrainId());
            assertEquals("t_floor", loaded.get(2, 2).getTerrainId());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void fixtureLoadsTenByTenMap() throws Exception {
        final Path fixture = fixtureMapPath();
        final MapGrid loaded = MapFileIO.load(fixture);

        assertEquals(10, loaded.width());
        assertEquals(10, loaded.height());
        assertEquals("t_dirt", loaded.getDefaultTerrainId());
        assertEquals("t_grass", loaded.get(0, 0).getTerrainId());
        assertEquals("t_floor", loaded.get(9, 9).getTerrainId());
    }

    @Test
    void invalidTerrainLengthThrowsExpectedMessage() throws Exception {
        final Path bad = Files.createTempFile("nextgen-map-bad-length-", ".json");
        final String json = "{\n"
            + "  \"format\": \"cdda-bn-nextgen-map\",\n"
            + "  \"version\": 1,\n"
            + "  \"width\": 2,\n"
            + "  \"height\": 2,\n"
            + "  \"default_terrain\": \"t_dirt\",\n"
            + "  \"terrain\": [\"t_dirt\"],\n"
            + "  \"furniture\": null\n"
            + "}\n";
        Files.write(bad, json.getBytes());
        try {
            final IOException error = assertThrows(IOException.class, () -> MapFileIO.load(bad));
            assertTrue(error.getMessage().contains("expected 4 but got 1"));
        } finally {
            Files.deleteIfExists(bad);
        }
    }

    @Test
    void unsupportedVersionIsRejected() throws Exception {
        final Path bad = Files.createTempFile("nextgen-map-bad-version-", ".json");
        final String json = "{\n"
            + "  \"format\": \"cdda-bn-nextgen-map\",\n"
            + "  \"version\": 2,\n"
            + "  \"width\": 1,\n"
            + "  \"height\": 1,\n"
            + "  \"default_terrain\": \"t_dirt\",\n"
            + "  \"terrain\": [\"t_dirt\"],\n"
            + "  \"furniture\": null\n"
            + "}\n";
        Files.write(bad, json.getBytes());
        try {
            final IOException error = assertThrows(IOException.class, () -> MapFileIO.load(bad));
            assertTrue(error.getMessage().contains("Unsupported map version"));
        } finally {
            Files.deleteIfExists(bad);
        }
    }

    private static Path fixtureMapPath() throws URISyntaxException {
        final URL url = MapFileIOTest.class.getResource("/maps/test_10x10.json");
        assertNotNull(url, "fixture maps/test_10x10.json missing from test classpath");
        return Paths.get(url.toURI());
    }
}
