package io.gdx.cdda.bn.nextgen.tileset.load;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;
import io.gdx.cdda.bn.nextgen.tileset.GfxPaths;
import io.gdx.cdda.bn.nextgen.tileset.TilesetDiscovery;
import io.gdx.cdda.bn.nextgen.tileset.TilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetDiscovery;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetRegistry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TilesetLoaderModTest {

    @BeforeAll
    static void initGdx() {
        GdxTestSupport.initIfNeeded();
    }

    @Test
    void mergesCompatibleModSpritesAndOverridesTileId() throws Exception {
        final String rootProperty = System.getProperty(GfxPaths.GFX_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.gfx.roots=... to run");

        final TilesetRegistry registry = TilesetDiscovery.build();
        assumeTrue(registry.contains("hoder"), "hoder tileset not in gfx roots");

        GdxTestSupport.runOnGdxThread(() -> {
            final Path modDir = fixtureModDir("hoder_patch");
            writeModSheet(modDir.resolve("mod_sheet.png"));

            final ModTilesetRegistry modTilesets = ModTilesetRegistry.empty();
            ModTilesetDiscovery.registerJsonFile(modDir.resolve("mod_tileset.json"), modTilesets);

            final LoadedTileset baseOnly = TilesetLoader.load(registry, "hoder");
            final int baseSpriteCount;
            final int baseDirtFg;
            try {
                baseSpriteCount = baseOnly.getSpriteCount();
                baseDirtFg = baseOnly.findTile("t_dirt").orElseThrow(() ->
                    new AssertionError("missing t_dirt")).getForegroundSpriteIndex();
            } finally {
                baseOnly.dispose();
            }

            final LoadedTileset merged = TilesetLoader.load(
                registry, "hoder", TilesetLoadOptions.defaults(), modTilesets
            );
            try {
                assertEquals(2, merged.getSpriteCount() - baseSpriteCount, "mod sheet adds two sprites");
                assertTrue(merged.findTile("mod_slice4_tile").isPresent());
                final int modBaseIndex = merged.findTile("mod_slice4_tile").get().getForegroundSpriteIndex();
                assertEquals(modBaseIndex + 1, merged.findTile("t_dirt").get().getForegroundSpriteIndex());
                assertNotEquals(baseDirtFg, merged.findTile("t_dirt").get().getForegroundSpriteIndex());
                assertNotNull(merged.getForegroundTexture("mod_slice4_tile"));
            } finally {
                merged.dispose();
            }
        });
    }

    @Test
    void skipsIncompatibleMod() throws Exception {
        final String rootProperty = System.getProperty(GfxPaths.GFX_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.gfx.roots=... to run");

        final TilesetRegistry registry = TilesetDiscovery.build();
        assumeTrue(registry.contains("hoder"));

        GdxTestSupport.runOnGdxThread(() -> {
            final Path modDir = fixtureModDir("hoder_patch");
            writeModSheet(modDir.resolve("mod_sheet.png"));

            final ModTilesetRegistry wrongCompat = ModTilesetRegistry.empty();
            final Path json = modDir.resolve("mod_tileset.json");
            final String patched = new String(Files.readAllBytes(json), StandardCharsets.UTF_8)
                .replace("\"hoder\"", "\"not_hoder\"");
            final Path tempJson = modDir.resolve("wrong_compat.json");
            Files.write(tempJson, patched.getBytes(StandardCharsets.UTF_8));
            ModTilesetDiscovery.registerJsonFile(tempJson, wrongCompat);

            final LoadedTileset withoutMod = TilesetLoader.load(registry, "hoder");
            final LoadedTileset withSkippedMod = TilesetLoader.load(
                registry, "hoder", TilesetLoadOptions.defaults(), wrongCompat
            );
            try {
                assertEquals(withoutMod.getSpriteCount(), withSkippedMod.getSpriteCount());
                assertEquals(
                    withoutMod.findTile("t_dirt").get().getForegroundSpriteIndex(),
                    withSkippedMod.findTile("t_dirt").get().getForegroundSpriteIndex()
                );
            } finally {
                withoutMod.dispose();
                withSkippedMod.dispose();
            }
        });
    }

    private static void writeModSheet(final Path pngPath) throws Exception {
        if (Files.exists(pngPath)) {
            return;
        }
        final Pixmap pixmap = new Pixmap(32, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 0f, 0f, 1f);
        pixmap.fillRectangle(0, 0, 16, 16);
        pixmap.setColor(0f, 1f, 0f, 1f);
        pixmap.fillRectangle(16, 0, 16, 16);
        try {
            PixmapIO.writePNG(Gdx.files.absolute(pngPath.toAbsolutePath().toString()), pixmap);
        } finally {
            pixmap.dispose();
        }
    }

    private static Path fixtureModDir(final String name) throws URISyntaxException {
        final URL url = TilesetLoaderModTest.class.getResource("/mod-fixtures/" + name);
        assertNotNull(url, "missing mod fixture dir");
        return Paths.get(url.toURI());
    }
}
