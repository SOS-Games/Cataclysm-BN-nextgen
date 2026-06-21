package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataScanResult;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonDataScannerTest {

    @Test
    void listsJsonFilesUnderScanSubdirsOnly() throws Exception {
        final Path jsonRoot = fixtureJsonRoot();
        final List<Path> files = JsonDataScanner.listJsonFiles(
            jsonRoot,
            Collections.singletonList("furniture_and_terrain")
        );

        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("terrain-sample.json")));
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("deep.json")));
        assertTrue(files.stream().noneMatch(p -> p.toString().replace('\\', '/').contains("/other/")));
    }

    @Test
    void parsesArrayEnvelopeAndSkipsEntriesWithoutType() throws Exception {
        final Path file = fixtureJsonRoot().resolve("furniture_and_terrain/terrain-sample.json");
        final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);

        assertEquals(3, objects.size());
        assertEquals("terrain", objects.get(0).getType());
        assertEquals("furniture", objects.get(1).getType());
        assertEquals("GUN", objects.get(2).getType());
        assertEquals("t_fixture_floor", objects.get(0).getRoot().getString("id"));
    }

    @Test
    void parsesSingleObjectEnvelope() throws Exception {
        final Path file = fixtureJsonRoot().resolve("furniture_and_terrain/nested/deep.json");
        final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);

        assertEquals(1, objects.size());
        assertEquals("terrain", objects.get(0).getType());
        assertEquals("t_fixture_single", objects.get(0).getRoot().getString("id"));
    }

    @Test
    void scanCountsTypedObjectsFromFixtureRoot() throws Exception {
        final Path dataRoot = fixtureDataRoot();
        final GameDataLoadOptions options = GameDataLoadOptions.fromRoots(
            Collections.singletonList(dataRoot)
        );
        final GameDataScanResult result = GameDataLoader.scan(options);

        assertEquals(4, result.getJsonFiles().size());
        assertEquals(6, result.getTotalObjects());
        assertEquals(2, result.countObjects("terrain"));
        assertEquals(1, result.countObjects("furniture"));
        assertEquals(1, result.countObjects("GUN"));
        assertEquals(1, result.countObjects("item_group"));
        assertEquals(1, result.countObjects("monstergroup"));
    }

    private static Path fixtureDataRoot() throws URISyntaxException {
        final URL url = JsonDataScannerTest.class.getResource("/gamedata-fixtures");
        assertNotNull(url, "fixture gamedata-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }

    private static Path fixtureJsonRoot() throws URISyntaxException {
        return fixtureDataRoot().resolve("json");
    }
}
