package io.gdx.cdda.bn.nextgen.gamedata;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DataPathsTest {

    @Test
    void resolvesFixtureDataRootFromProperty() throws Exception {
        final Path fixtureRoot = fixtureDataRoot();
        final String previous = System.getProperty(DataPaths.DATA_ROOTS_PROPERTY);
        System.setProperty(DataPaths.DATA_ROOTS_PROPERTY, fixtureRoot.toString());
        try {
            final Path jsonRoot = DataPaths.coreJsonRoot();
            assertNotNull(jsonRoot);
            assertTrue(Files.isDirectory(jsonRoot.resolve("furniture_and_terrain")));
        } finally {
            restoreProperty(DataPaths.DATA_ROOTS_PROPERTY, previous);
        }
    }

    @Test
    void resolvesSiblingBnDataWhenPresent() {
        final Path bnData = Paths.get("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(Files.isDirectory(bnData), "Cataclysm-BN/data not found beside nextgen");

        final String previous = System.getProperty(DataPaths.DATA_ROOTS_PROPERTY);
        System.clearProperty(DataPaths.DATA_ROOTS_PROPERTY);
        try {
            final Path jsonRoot = DataPaths.coreJsonRoot();
            assertNotNull(jsonRoot);
            assertTrue(Files.isDirectory(jsonRoot));
            assertTrue(Files.isDirectory(jsonRoot.resolve("furniture_and_terrain")));
        } finally {
            restoreProperty(DataPaths.DATA_ROOTS_PROPERTY, previous);
        }
    }

    @Test
    void integrationScanBnJsonWhenPropertySet() throws Exception {
        final String rootProperty = System.getProperty(DataPaths.DATA_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.data.roots=... to run");

        final Path jsonRoot = DataPaths.coreJsonRoot();
        assertNotNull(jsonRoot);
        assertTrue(Files.isDirectory(jsonRoot.resolve("furniture_and_terrain")));
    }

    private static Path fixtureDataRoot() throws URISyntaxException {
        final URL url = DataPathsTest.class.getResource("/gamedata-fixtures");
        assertNotNull(url, "fixture gamedata-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }

    private static void restoreProperty(final String name, final String previous) {
        if (previous == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previous);
        }
    }
}
