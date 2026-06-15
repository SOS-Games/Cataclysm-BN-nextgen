package io.gdx.cdda.bn.nextgen.tileset;

import com.badlogic.gdx.utils.JsonValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TilesetDiscoveryTest {

    @Test
    void discoversRetrodaysFromFixtureGfxRoot() throws Exception {
        final Path gfxRoot = fixtureGfxRoot();
        final TilesetRegistry registry = TilesetDiscovery.build(
            Collections.singletonList(gfxRoot),
            Collections.emptyList()
        );

        assertTrue(registry.contains("retrodays"));
        assertEquals(
            gfxRoot.resolve("RetroDaysTileset").toAbsolutePath().normalize(),
            registry.findDirectory("retrodays").orElseThrow().toAbsolutePath().normalize()
        );
        assertTrue(registry.getOptions().stream().anyMatch(o -> "retrodays".equals(o.getId())));
    }

    @Test
    void discoversNestedTilesetDirectory() throws Exception {
        final Path gfxRoot = fixtureGfxRoot();
        final TilesetRegistry registry = TilesetDiscovery.build(
            Collections.singletonList(gfxRoot),
            Collections.emptyList()
        );

        assertTrue(registry.contains("nestedpack"));
        assertTrue(
            registry.findDirectory("nestedpack").orElseThrow().toString().replace('\\', '/')
                .endsWith("gfx-fixtures/deep/NestedPack")
        );
    }

    @Test
    void loadManifestReadsJsonAndTilesetKeys() throws Exception {
        final Path gfxRoot = fixtureGfxRoot();
        final Path manifest = gfxRoot.resolve("RetroDaysTileset/tileset.txt");
        final TilesetManifestParsers.LoadManifest load =
            TilesetManifestParsers.parseLoadManifest(manifest);

        assertEquals("tile_config.json", load.getJsonRelativePath());
        assertEquals("tiles.png", load.getImageRelativePath());
    }

    @Test
    void discoveryParserStopsAtView() throws Exception {
        final Path gfxRoot = fixtureGfxRoot();
        final Path manifest = gfxRoot.resolve("RetroDaysTileset/tileset.txt");
        final TilesetOption option =
            TilesetManifestParsers.parseDiscoveryManifest(manifest).orElseThrow();

        assertEquals("retrodays", option.getId());
        assertEquals("RetroDays", option.getDisplayName());
    }

    @Test
    void opensTileConfigJson() throws Exception {
        final Path gfxRoot = fixtureGfxRoot();
        final TilesetRegistry registry = TilesetDiscovery.build(
            Collections.singletonList(gfxRoot),
            Collections.emptyList()
        );

        final TilesetConfigLoader.LoadedTilesetConfig loaded =
            TilesetConfigLoader.loadConfig(registry, "retrodays");

        final JsonValue root = loaded.getConfigRoot();
        assertNotNull(root);
        assertTrue(root.has("tile_info"));
        assertEquals(
            gfxRoot.resolve("RetroDaysTileset/tile_config.json").toAbsolutePath().normalize(),
            loaded.getPaths().getJsonPath().toAbsolutePath().normalize()
        );
    }

    @Test
    void unknownTilesetIdThrows() throws Exception {
        final Path gfxRoot = fixtureGfxRoot();
        final TilesetRegistry registry = TilesetDiscovery.build(
            Collections.singletonList(gfxRoot),
            Collections.emptyList()
        );

        assertThrows(IllegalArgumentException.class, () ->
            TilesetConfigLoader.resolvePaths(registry, "not_a_real_pack")
        );
    }

    @Test
    void integrationScanBnGfxWhenPropertySet() throws IOException {
        final String rootProperty = System.getProperty(GfxPaths.GFX_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.gfx.roots=... to run");

        final TilesetRegistry registry = TilesetDiscovery.build();
        assertFalse(registry.getDirectoriesById().isEmpty());

        final String firstId = registry.getOptions().get(0).getId();
        assumeTrue(registry.contains(firstId));
        final TilesetConfigLoader.LoadedTilesetConfig loaded =
            TilesetConfigLoader.loadConfig(registry, firstId);
        assertTrue(loaded.getConfigRoot().has("tile_info"));
    }

    private static Path fixtureGfxRoot() throws URISyntaxException {
        final URL url = TilesetDiscoveryTest.class.getResource("/gfx-fixtures");
        assertNotNull(url, "fixture gfx-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }
}
