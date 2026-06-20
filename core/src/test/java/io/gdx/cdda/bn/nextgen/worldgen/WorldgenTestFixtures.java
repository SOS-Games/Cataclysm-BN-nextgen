package io.gdx.cdda.bn.nextgen.worldgen;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Classpath fixtures for worldgen unit tests. */
public final class WorldgenTestFixtures {

    private WorldgenTestFixtures() {}

    public static Path fixtureDataRoot() throws URISyntaxException {
        final URL url = WorldgenTestFixtures.class.getResource("/mapgen-fixtures");
        assertNotNull(url, "mapgen-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }

    public static Path overmapFixturePath() throws URISyntaxException {
        final URL url = WorldgenTestFixtures.class.getResource("/worldgen-fixtures/overmaps/test_8x8.json");
        assertNotNull(url, "worldgen overmap fixture missing from test classpath");
        return Paths.get(url.toURI());
    }
}
