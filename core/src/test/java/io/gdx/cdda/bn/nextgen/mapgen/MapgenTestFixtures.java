package io.gdx.cdda.bn.nextgen.mapgen;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Shared classpath fixtures for mapgen unit tests. */
public final class MapgenTestFixtures {

    private MapgenTestFixtures() {}

    public static Path fixtureDataRoot() throws URISyntaxException {
        final URL url = MapgenTestFixtures.class.getResource("/mapgen-fixtures");
        assertNotNull(url, "mapgen-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }
}
