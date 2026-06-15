package io.gdx.cdda.bn.nextgen.tileset.load;

import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;
import io.gdx.cdda.bn.nextgen.tileset.GfxPaths;
import io.gdx.cdda.bn.nextgen.tileset.TilesetDiscovery;
import io.gdx.cdda.bn.nextgen.tileset.TilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.validate.PostLoadValidator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TilesetLoaderTest {

    @BeforeAll
    static void initGdx() {
        GdxTestSupport.initIfNeeded();
    }

    @Test
    void emptyTilesNewYieldsZeroSprites() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final Path gfxRoot = fixtureGfxRoot();
            final TilesetRegistry registry = TilesetDiscovery.build(
                Collections.singletonList(gfxRoot),
                Collections.emptyList()
            );

            final LoadedTileset loaded = TilesetLoader.load(registry, "retrodays");
            try {
                assertEquals(1, loaded.getSpriteCount(), "synthesized highlight_item sprite");
                assertEquals(0, loaded.getTiles().size() - 1, "only highlight_item tile id");
                assertEquals(10, loaded.getTileInfo().getWidth());
                assertTrue(loaded.findTile(PostLoadValidator.HIGHLIGHT_ITEM_ID).isPresent());
            } finally {
                loaded.dispose();
            }
        });
    }

    @Test
    void configWithoutSheetsYieldsZeroSprites() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final Path gfxRoot = fixtureGfxRoot();
            final TilesetRegistry registry = TilesetDiscovery.build(
                Collections.singletonList(gfxRoot),
                Collections.emptyList()
            );

            final LoadedTileset loaded = TilesetLoader.load(registry, "nestedpack");
            try {
                assertEquals(1, loaded.getSpriteCount());
                assertTrue(loaded.findTile(PostLoadValidator.HIGHLIGHT_ITEM_ID).isPresent());
            } finally {
                loaded.dispose();
            }
        });
    }

    @Test
    void integrationLoadsHoderWhenBnGfxPresent() throws Exception {
        final String rootProperty = System.getProperty(GfxPaths.GFX_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.gfx.roots=... to run");

        final TilesetRegistry registry = TilesetDiscovery.build();
        assumeTrue(registry.contains("hoder"), "hoder tileset not in gfx roots");

        GdxTestSupport.runOnGdxThread(() -> {
            final LoadedTileset loaded = TilesetLoader.load(registry, "hoder");
            try {
                assertTrue(loaded.getSpriteCount() > 0, "expected sprites from hodertiles.png");
                assertNotNull(loaded.getTexture(0));
                assertEquals(16, loaded.getTileInfo().getWidth());
                assertTrue(loaded.findTile("t_dirt").isPresent(), "hoder defines t_dirt");
                assertEquals(2, loaded.findTile("t_dirt").get().getForegroundSpriteIndex());
                assertNotNull(loaded.getForegroundTexture("t_dirt"));
                assertTrue(loaded.findTile("unknown").isPresent(), "hoder defines unknown");
            } finally {
                loaded.dispose();
            }
        });
    }

    @Test
    void integrationLoadsRetrodaysWhenBnGfxPresent() throws Exception {
        final String rootProperty = System.getProperty(GfxPaths.GFX_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.gfx.roots=... to run");

        final TilesetRegistry registry = TilesetDiscovery.build();
        assumeTrue(registry.contains("retrodays"), "retrodays tileset not in gfx roots");

        GdxTestSupport.runOnGdxThread(() -> {
            final LoadedTileset loaded = TilesetLoader.load(registry, "retrodays");
            try {
                assertTrue(loaded.getSpriteCount() > 3000, "RetroDays tiles.png should yield thousands of sprites");
                assertTrue(loaded.getTiles().size() > 1000);
                assertNotNull(loaded.getTexture(1));
                assertEquals(2f, loaded.getTileInfo().getPixelScale(), 0.001f);
            } finally {
                loaded.dispose();
            }
        });
    }

    private static Path fixtureGfxRoot() throws URISyntaxException {
        final URL url = TilesetLoaderTest.class.getResource("/gfx-fixtures");
        assertNotNull(url, "fixture gfx-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }
}
