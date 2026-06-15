package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteTextureTables;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheetTextureUploaderTest {

    @Test
    void uploadsFourCellsFrom64x64Sheet() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final SpriteTextureTables tables = new SpriteTextureTables();
            final Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            pixmap.setColor(1f, 0f, 0f, 1f);
            pixmap.fillRectangle(0, 0, 32, 32);
            pixmap.setColor(0f, 1f, 0f, 1f);
            pixmap.fillRectangle(32, 0, 32, 32);
            pixmap.setColor(0f, 0f, 1f, 1f);
            pixmap.fillRectangle(0, 32, 32, 32);
            pixmap.setColor(1f, 1f, 0f, 1f);
            pixmap.fillRectangle(32, 32, 32, 32);

            try {
                final int size = SheetTextureUploader.uploadSheet(
                    pixmap, 32, 32, 0, 4096, 4096, tables, TilesetLoadOptions.defaults()
                );
                assertEquals(4, size);
                assertFalse(tables.getTable(TilesetFxType.NONE).get(0).isEmpty());
                assertFalse(tables.getTable(TilesetFxType.NONE).get(3).isEmpty());
                final TextureRegion quadrant = tables.getRegion(2, TilesetFxType.NONE);
                assertNotNull(quadrant);
                assertEquals(32, quadrant.getRegionWidth());
                assertEquals(4, tables.size());
            } finally {
                pixmap.dispose();
                tables.dispose();
            }
        });
    }

    @Test
    void bakesEightParallelTablesWithDifferentPixels() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final SpriteTextureTables tables = new SpriteTextureTables();
            final Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            pixmap.setColor(1f, 0f, 0f, 1f);
            pixmap.fill();

            try {
                SheetTextureUploader.uploadSheet(
                    pixmap, 32, 32, 0, 4096, 4096, tables, TilesetLoadOptions.defaults()
                );
                assertEquals(tables.getTable(TilesetFxType.NONE).size(), tables.getTable(TilesetFxType.NIGHT).size());
                final TextureRegion normal = tables.getRegion(0, TilesetFxType.NONE);
                final TextureRegion night = tables.getRegion(0, TilesetFxType.NIGHT);
                assertNotNull(normal);
                assertNotNull(night);
                assertEquals(normal.getRegionWidth(), night.getRegionWidth());
                assertNotEquals(
                    tables.getTable(TilesetFxType.NONE).get(0).getTexture(),
                    tables.getTable(TilesetFxType.NIGHT).get(0).getTexture()
                );
            } finally {
                pixmap.dispose();
                tables.dispose();
            }
        });
    }

    @Test
    void splitsWideAtlasAndKeepsIndicesContiguous() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final SpriteTextureTables tables = new SpriteTextureTables();
            final Pixmap pixmap = new Pixmap(512, 64, Pixmap.Format.RGBA8888);
            pixmap.setColor(1f, 1f, 1f, 1f);
            pixmap.fill();

            try {
                final int size = SheetTextureUploader.uploadSheet(
                    pixmap, 32, 32, 10, 256, 4096, tables, TilesetLoadOptions.defaults()
                );
                assertEquals(32, size);
                assertFalse(tables.getTable(TilesetFxType.NONE).get(10).isEmpty());
                assertFalse(tables.getTable(TilesetFxType.NONE).get(41).isEmpty());
                assertTrue(tables.getTable(TilesetFxType.NONE).get(9).isEmpty());
            } finally {
                pixmap.dispose();
                tables.dispose();
            }
        });
    }

    @Test
    void rejectsDuplicateGlobalIndex() throws Exception {
        GdxTestSupport.runOnGdxThread(() -> {
            final SpriteTextureTables tables = new SpriteTextureTables();
            final Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            pixmap.setColor(1f, 1f, 1f, 1f);
            pixmap.fill();

            try {
                SheetTextureUploader.uploadSheet(
                    pixmap, 32, 32, 0, 4096, 4096, tables, TilesetLoadOptions.defaults()
                );
                assertThrows(IllegalStateException.class, () ->
                    SheetTextureUploader.uploadSheet(
                        pixmap, 32, 32, 0, 4096, 4096, tables, TilesetLoadOptions.defaults()
                    )
                );
            } finally {
                pixmap.dispose();
                tables.dispose();
            }
        });
    }
}
