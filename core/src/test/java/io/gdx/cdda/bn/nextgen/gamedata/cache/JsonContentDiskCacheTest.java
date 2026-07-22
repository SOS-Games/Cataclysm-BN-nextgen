package io.gdx.cdda.bn.nextgen.gamedata.cache;

import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonContentDiskCacheTest {

    @TempDir
    Path tempDir;

    private String previousEnabled;
    private String previousDir;

    @BeforeEach
    void enableCache() {
        previousEnabled = System.getProperty(ContentCachePaths.CACHE_ENABLED_PROPERTY);
        previousDir = System.getProperty(ContentCachePaths.CACHE_DIR_PROPERTY);
        System.setProperty(ContentCachePaths.CACHE_ENABLED_PROPERTY, "true");
        System.setProperty(ContentCachePaths.CACHE_DIR_PROPERTY, tempDir.resolve("cache").toString());
    }

    @AfterEach
    void restoreProperties() {
        restore(ContentCachePaths.CACHE_ENABLED_PROPERTY, previousEnabled);
        restore(ContentCachePaths.CACHE_DIR_PROPERTY, previousDir);
    }

    @Test
    void recordsOnMissThenHitsWithoutRereadingDisk() throws Exception {
        final Path dataRoot = tempDir.resolve("data");
        final Path jsonFile = dataRoot.resolve("json").resolve("sample.json");
        Files.createDirectories(jsonFile.getParent());
        Files.writeString(
            jsonFile,
            "[{\"type\":\"terrain\",\"id\":\"t_test\",\"name\":\"Test\"}]",
            StandardCharsets.UTF_8
        );

        final List<Path> roots = Collections.singletonList(dataRoot);
        final List<String> mods = Collections.singletonList("testmod");

        JsonContentDiskCache.withSession("mapgen", roots, mods, () -> {
            assertFalse(JsonContentDiskCache.activePack().isPresent());
            final List<Path> listed = JsonDataScanner.listJsonFiles(dataRoot.resolve("json"), Collections.emptyList());
            assertEquals(1, listed.size());
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(listed.get(0));
            assertEquals(1, objects.size());
            assertEquals("terrain", objects.get(0).getType());
        });

        assertTrue(Files.isRegularFile(ContentCachePaths.stampFile("mapgen")));
        assertTrue(Files.isRegularFile(ContentCachePaths.packFile("mapgen")));
        assertTrue(JsonContentDiskCache.tryLoad("mapgen", roots, mods).isPresent());

        JsonContentDiskCache.withSession("mapgen", roots, mods, () -> {
            assertTrue(JsonContentDiskCache.activePack().isPresent());
            final List<Path> listed = JsonDataScanner.listJsonFiles(dataRoot.resolve("json"), Collections.emptyList());
            assertEquals(1, listed.size());
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(listed.get(0));
            assertEquals("terrain", objects.get(0).getType());
        });
    }

    @Test
    void stampMismatchOnMtimeChangeForcesMiss() throws Exception {
        final Path dataRoot = tempDir.resolve("data2");
        final Path jsonFile = dataRoot.resolve("json").resolve("a.json");
        Files.createDirectories(jsonFile.getParent());
        Files.writeString(
            jsonFile,
            "[{\"type\":\"furniture\",\"id\":\"f_x\",\"name\":\"X\"}]",
            StandardCharsets.UTF_8
        );

        final List<Path> roots = Collections.singletonList(dataRoot);
        final List<String> mods = Collections.emptyList();

        JsonContentDiskCache.withSession("worldgen", roots, mods, () -> {
            JsonDataScanner.parseFile(jsonFile);
        });
        assertTrue(JsonContentDiskCache.tryLoad("worldgen", roots, mods).isPresent());

        Files.setLastModifiedTime(jsonFile, FileTime.fromMillis(System.currentTimeMillis() + 60_000L));
        assertFalse(JsonContentDiskCache.tryLoad("worldgen", roots, mods).isPresent());
    }

    @Test
    void differentModsInvalidateStamp() throws Exception {
        final Path dataRoot = tempDir.resolve("data3");
        final Path jsonFile = dataRoot.resolve("json").resolve("b.json");
        Files.createDirectories(jsonFile.getParent());
        Files.writeString(jsonFile, "[{\"type\":\"terrain\",\"id\":\"t_y\",\"name\":\"Y\"}]", StandardCharsets.UTF_8);

        final List<Path> roots = Collections.singletonList(dataRoot);
        JsonContentDiskCache.withSession("mapgen", roots, Collections.singletonList("mod_a"), () -> {
            JsonDataScanner.parseFile(jsonFile);
        });

        assertTrue(JsonContentDiskCache.tryLoad("mapgen", roots, Collections.singletonList("mod_a")).isPresent());
        assertFalse(JsonContentDiskCache.tryLoad("mapgen", roots, Collections.singletonList("mod_b")).isPresent());
    }

    private static void restore(final String key, final String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}
