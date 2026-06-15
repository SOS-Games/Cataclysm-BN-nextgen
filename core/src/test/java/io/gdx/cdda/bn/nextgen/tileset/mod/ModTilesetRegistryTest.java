package io.gdx.cdda.bn.nextgen.tileset.mod;

import com.badlogic.gdx.utils.JsonReader;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModTilesetRegistryTest {

    @Test
    void registersCompatibilityFromObject() throws Exception {
        final ModTilesetRegistry registry = ModTilesetRegistry.empty();
        final Path json = fixturePath("hoder_patch/mod_tileset.json");
        ModTilesetDiscovery.registerJsonFile(json, registry);

        assertEquals(1, registry.getEntries().size());
        final ModTilesetEntry entry = registry.getEntries().get(0);
        assertEquals(1, entry.getNumInFile());
        assertTrue(entry.isCompatible("hoder"));
        assertFalse(entry.isCompatible("retrodays"));
        assertEquals(json.getParent(), entry.getBasePath());
        assertEquals(json, entry.getFullPath());
    }

    @Test
    void registersTwoEntriesFromArrayFile() throws Exception {
        final ModTilesetRegistry registry = ModTilesetRegistry.empty();
        final Path json = fixturePath("multi_entry/mod_tileset.json");
        ModTilesetDiscovery.registerJsonFile(json, registry);

        assertEquals(2, registry.getEntries().size());
        assertEquals(1, registry.getEntries().get(0).getNumInFile());
        assertEquals(2, registry.getEntries().get(1).getNumInFile());
        assertTrue(registry.getEntries().get(0).isCompatible("tileset_a"));
        assertTrue(registry.getEntries().get(1).isCompatible("tileset_b"));
    }

    @Test
    void resolveConfigObjectPicksNthModTileset() throws Exception {
        final Path json = fixturePath("multi_entry/mod_tileset.json");
        final byte[] bytes = java.nio.file.Files.readAllBytes(json);
        final com.badlogic.gdx.utils.JsonValue root =
            new JsonReader().parse(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));

        assertNotNull(ModTilesetMerger.resolveConfigObject(root, 1));
        assertNotNull(ModTilesetMerger.resolveConfigObject(root, 2));
        assertNull(ModTilesetMerger.resolveConfigObject(root, 3));
    }

    @Test
    void clearRemovesEntries() throws Exception {
        final ModTilesetRegistry registry = ModTilesetRegistry.empty();
        ModTilesetDiscovery.registerJsonFile(fixturePath("hoder_patch/mod_tileset.json"), registry);
        assertEquals(1, registry.getEntries().size());
        registry.clear();
        assertTrue(registry.getEntries().isEmpty());
    }

    private static Path fixturePath(final String relative) throws URISyntaxException {
        final URL url = ModTilesetRegistryTest.class.getResource("/mod-fixtures/" + relative);
        assertNotNull(url, "missing fixture: " + relative);
        return Paths.get(url.toURI());
    }
}
