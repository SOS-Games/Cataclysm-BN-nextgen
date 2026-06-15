package io.gdx.cdda.bn.nextgen.tileset.load;

import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;
import io.gdx.cdda.bn.nextgen.tileset.GfxPaths;
import io.gdx.cdda.bn.nextgen.tileset.TilesetDiscovery;
import io.gdx.cdda.bn.nextgen.tileset.TilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;
import io.gdx.cdda.bn.nextgen.tileset.validate.PostLoadValidator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TilesetLoaderDynamicAtlasTest {

    @BeforeAll
    static void initGdx() {
        GdxTestSupport.initIfNeeded();
    }

    @Test
    void dynamicLoadCreatesHighlightLookupEntry() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final Path gfxRoot = fixtureGfxRoot();
            final TilesetRegistry registry = TilesetDiscovery.build(
                Collections.singletonList(gfxRoot),
                Collections.emptyList()
            );
            final LoadedTileset loaded = TilesetLoader.load(
                registry,
                "retrodays",
                TilesetLoadOptions.dynamicAtlas()
            );
            try {
                assertTrue(loaded.isDynamicAtlas());
                assertNull(loaded.getTextures().getBakedTables());
                assertTrue(loaded.findTile(PostLoadValidator.HIGHLIGHT_ITEM_ID).isPresent());
                assertNotNull(loaded.getTexture(0, TilesetFxType.NONE));
                assertTrue(loaded.getTextures().getTileLookup().findBase(0).isPresent());
            } finally {
                loaded.dispose();
            }
        });
    }

    @Test
    void bakedModeStillUsesEightTables() throws Exception {
        final String rootProperty = System.getProperty(GfxPaths.GFX_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.gfx.roots=... to run");

        final TilesetRegistry registry = TilesetDiscovery.build();
        assumeTrue(registry.contains("hoder"));

        GdxTestSupport.runOnGdxThread(() -> {
            final LoadedTileset loaded = TilesetLoader.load(registry, "hoder");
            try {
                assertTrue(!loaded.isDynamicAtlas());
                assertNotNull(loaded.getTextures().getBakedTables());
            } finally {
                loaded.dispose();
            }
        });
    }

    private static Path fixtureGfxRoot() throws URISyntaxException {
        final URL url = TilesetLoaderDynamicAtlasTest.class.getResource("/gfx-fixtures");
        assertNotNull(url, "fixture gfx-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }
}
