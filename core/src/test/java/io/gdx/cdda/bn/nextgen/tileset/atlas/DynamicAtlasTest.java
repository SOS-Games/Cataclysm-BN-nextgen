package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;

import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.TileLookup;
import io.gdx.cdda.bn.nextgen.tileset.model.TileLookupKey;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetTextures;
import io.gdx.cdda.bn.nextgen.tileset.model.WarpCache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicAtlasTest {

    @Test
    void loadDoesNotCreateBakedTables() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final TilesetTextures textures = TilesetTextures.create(
                TilesetLoadOptions.dynamicAtlas(),
                32,
                32
            );
            try {
                assertTrue(textures.isDynamicAtlas());
                assertNull(textures.getBakedTables());
            } finally {
                textures.dispose();
            }
        });
    }

    @Test
    void uploadCreatesBaseLookupEntries() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final TilesetTextures textures = TilesetTextures.create(
                TilesetLoadOptions.dynamicAtlas(),
                32,
                32
            );
            textures.beginLoad();
            final Pixmap sheet = solidSheet(64, 64, 1f, 0f, 0f, 1f);
            try {
                DynamicSheetUploader.uploadSheet(
                    sheet, 32, 32, 0, textures.getDynamicAtlas(), textures.getTileLookup()
                );
                textures.finishLoad();
                assertTrue(textures.getTileLookup().findBase(0).isPresent());
                assertTrue(textures.getTileLookup().findBase(3).isPresent());
                assertNotNull(textures.getRegion(0, TilesetFxType.NONE));
            } finally {
                sheet.dispose();
                textures.dispose();
            }
        });
    }

    @Test
    void lazyNightCompositeCachesSecondLookup() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final TilesetTextures textures = TilesetTextures.create(
                TilesetLoadOptions.dynamicAtlas(),
                32,
                32
            );
            textures.beginLoad();
            final Pixmap sheet = solidSheet(32, 32, 1f, 0f, 0f, 1f);
            try {
                DynamicSheetUploader.uploadSheet(
                    sheet, 32, 32, 0, textures.getDynamicAtlas(), textures.getTileLookup()
                );
                textures.finishLoad();
                final int lookupBefore = textures.getTileLookup().size();
                final com.badlogic.gdx.graphics.g2d.TextureRegion first =
                    textures.getRegion(0, TilesetFxType.NIGHT);
                final int lookupAfterFirst = textures.getTileLookup().size();
                final com.badlogic.gdx.graphics.g2d.TextureRegion second =
                    textures.getRegion(0, TilesetFxType.NIGHT);
                assertNotNull(first);
                assertNotNull(second);
                assertEquals(lookupAfterFirst, textures.getTileLookup().size());
                assertTrue(lookupAfterFirst > lookupBefore);
            } finally {
                sheet.dispose();
                textures.dispose();
            }
        });
    }

    @Test
    void identicalCellsShareAtlasPlacementButDistinctIndices() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final DynamicAtlas atlas = new DynamicAtlas(4096, 4096, 32, 32);
            final TileLookup lookup = new TileLookup();
            atlas.startBatch();
            final Pixmap sheet = new Pixmap(64, 32, Pixmap.Format.RGBA8888);
            sheet.setColor(0f, 1f, 0f, 1f);
            sheet.fill();
            try {
                DynamicSheetUploader.uploadSheet(sheet, 32, 32, 0, atlas, lookup);
                atlas.endBatch();
                final io.gdx.cdda.bn.nextgen.tileset.atlas.AtlasPlacement placement0 =
                    lookup.findBase(0).get().getPlacement();
                final io.gdx.cdda.bn.nextgen.tileset.atlas.AtlasPlacement placement1 =
                    lookup.findBase(1).get().getPlacement();
                assertEquals(placement0.getTexture(), placement1.getTexture());
                assertEquals(placement0.getX(), placement1.getX());
                assertEquals(placement0.getY(), placement1.getY());
                assertSame(placement0, placement1);
                assertTrue(lookup.findBase(0).isPresent());
                assertTrue(lookup.findBase(1).isPresent());
            } finally {
                sheet.dispose();
                atlas.dispose();
            }
        });
    }

    @Test
    void readbackReturnsPixelsAfterLoad() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final TilesetTextures textures = TilesetTextures.create(
                TilesetLoadOptions.dynamicAtlas(),
                32,
                32
            );
            textures.beginLoad();
            final Pixmap sheet = solidSheet(32, 32, 0f, 0f, 1f, 1f);
            try {
                DynamicSheetUploader.uploadSheet(
                    sheet, 32, 32, 0, textures.getDynamicAtlas(), textures.getTileLookup()
                );
                textures.finishLoad();
                final Pixmap pixels = textures.copyBaseSpritePixels(0).orElse(null);
                assertNotNull(pixels);
                try {
                    final int rgba = pixels.getPixel(0, 0);
                    assertTrue((rgba & 0xff) > 0);
                } finally {
                    pixels.dispose();
                }
            } finally {
                sheet.dispose();
                textures.dispose();
            }
        });
    }

    @Test
    void warpCacheClearsBetweenCharacters() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final WarpCache cache = new WarpCache();
            final Pixmap warp = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            warp.setColor(1f, 0f, 0f, 1f);
            warp.fill();
            try {
                cache.registerWarpSurface(warp, 0, 0, true);
                assertEquals(1, cache.size());
                cache.clear();
                assertEquals(0, cache.size());
            } finally {
                warp.dispose();
            }
        });
    }

    @Test
    void warpHashZeroStoredAsOne() {
        assertEquals(1L, WarpCache.normalizeWarpContentHash(TileLookupKey.NO_WARP));
    }

    @Test
    void getOrDefaultWithNoWarpUsesFxCompositeOnly() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final TilesetTextures textures = TilesetTextures.create(
                TilesetLoadOptions.dynamicAtlas(),
                32,
                32
            );
            textures.beginLoad();
            final Pixmap sheet = solidSheet(32, 32, 1f, 0f, 0f, 1f);
            try {
                DynamicSheetUploader.uploadSheet(
                    sheet, 32, 32, 0, textures.getDynamicAtlas(), textures.getTileLookup()
                );
                textures.finishLoad();
                final com.badlogic.gdx.graphics.g2d.TextureRegion base =
                    textures.getRegion(0, TilesetFxType.NONE);
                final com.badlogic.gdx.graphics.g2d.TextureRegion night =
                    textures.getRegion(0, TilesetFxType.NIGHT);
                assertNotNull(base);
                assertNotNull(night);
                assertTrue(
                    base.getRegionX() != night.getRegionX()
                        || base.getRegionY() != night.getRegionY()
                        || textures.getTileLookup().size() > 1
                );
            } finally {
                sheet.dispose();
                textures.dispose();
            }
        });
    }

    private static Pixmap solidSheet(
        final int width,
        final int height,
        final float red,
        final float green,
        final float blue,
        final float alpha
    ) {
        final Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(red, green, blue, alpha);
        pixmap.fill();
        return pixmap;
    }
}
