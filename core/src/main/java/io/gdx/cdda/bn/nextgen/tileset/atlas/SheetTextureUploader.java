package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteSlot;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteTextureTable;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteTextureTables;

import java.util.List;

/** Uploads decoded sheet pixmaps into parallel effect tables (units 06b, 06c). */
public final class SheetTextureUploader {

    private static final int MIN_TILE_X_COUNT = 128;
    private static final int MIN_TILE_Y_COUNT = MIN_TILE_X_COUNT * 2;

    private SheetTextureUploader() {}

    public static int uploadSheet(
        final Pixmap atlasPixmap,
        final int spriteWidth,
        final int spriteHeight,
        final int globalOffset,
        final int maxTextureWidth,
        final int maxTextureHeight,
        final SpriteTextureTables targets,
        final TilesetLoadOptions bakeOptions
    ) {
        final int atlasWidth = atlasPixmap.getWidth();
        final int atlasHeight = atlasPixmap.getHeight();
        final int expectedTileCount = AtlasGrid.expectedTileCount(
            atlasWidth, atlasHeight, spriteWidth, spriteHeight
        );
        targets.ensureCapacity(globalOffset + expectedTileCount);

        final int resolvedMaxWidth = AtlasChunkLayout.resolveMaxTextureWidth(
            maxTextureWidth, spriteWidth, MIN_TILE_X_COUNT
        );
        final int resolvedMaxHeight = AtlasChunkLayout.resolveMaxTextureHeight(
            maxTextureHeight, spriteHeight, MIN_TILE_Y_COUNT
        );

        final List<AtlasChunkLayout.ChunkRect> chunks = AtlasChunkLayout.computeChunks(
            atlasWidth,
            atlasHeight,
            spriteWidth,
            spriteHeight,
            resolvedMaxWidth,
            resolvedMaxHeight
        );

        final FilteredTableBake.Entry[] bakeEntries = FilteredTableBake.entriesFor(targets, bakeOptions);
        for (final AtlasChunkLayout.ChunkRect chunk : chunks) {
            uploadChunk(
                atlasPixmap,
                chunk,
                atlasWidth,
                spriteWidth,
                spriteHeight,
                globalOffset,
                bakeEntries
            );
        }

        return expectedTileCount;
    }

    private static void uploadChunk(
        final Pixmap atlasPixmap,
        final AtlasChunkLayout.ChunkRect chunk,
        final int tileAtlasWidth,
        final int spriteWidth,
        final int spriteHeight,
        final int globalOffset,
        final FilteredTableBake.Entry[] bakeEntries
    ) {
        final Pixmap chunkPixmap = PixmapSheetLoader.extractRegion(
            atlasPixmap,
            chunk.getX(),
            chunk.getY(),
            chunk.getWidth(),
            chunk.getHeight()
        );
        try {
            for (final FilteredTableBake.Entry bakeEntry : bakeEntries) {
                Pixmap uploadPixmap = chunkPixmap;
                boolean disposeUploadPixmap = false;
                if (bakeEntry.getFilter() != null) {
                    uploadPixmap = new Pixmap(
                        chunkPixmap.getWidth(),
                        chunkPixmap.getHeight(),
                        chunkPixmap.getFormat()
                    );
                    uploadPixmap.drawPixmap(chunkPixmap, 0, 0);
                    ColorPixelFilters.applyToPixmap(uploadPixmap, bakeEntry.getFilter());
                    disposeUploadPixmap = true;
                }
                try {
                    uploadChunkPixmap(
                        uploadPixmap,
                        chunk,
                        tileAtlasWidth,
                        spriteWidth,
                        spriteHeight,
                        globalOffset,
                        bakeEntry.getTable()
                    );
                } finally {
                    if (disposeUploadPixmap) {
                        uploadPixmap.dispose();
                    }
                }
            }
        } finally {
            chunkPixmap.dispose();
        }
    }

    private static void uploadChunkPixmap(
        final Pixmap chunkPixmap,
        final AtlasChunkLayout.ChunkRect chunk,
        final int tileAtlasWidth,
        final int spriteWidth,
        final int spriteHeight,
        final int globalOffset,
        final SpriteTextureTable target
    ) {
        final Texture texture = new Texture(chunkPixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        final int columns = chunk.getWidth() / spriteWidth;
        final int rows = chunk.getHeight() / spriteHeight;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                final int cellX = col * spriteWidth;
                final int cellY = row * spriteHeight;
                final int pixelX = chunk.getX() + cellX;
                final int pixelY = chunk.getY() + cellY;
                final int globalIndex = AtlasGrid.globalIndex(
                    pixelX,
                    pixelY,
                    tileAtlasWidth,
                    spriteWidth,
                    spriteHeight,
                    globalOffset
                );
                final SpriteSlot slot = SpriteSlot.of(
                    texture,
                    cellX,
                    cellY,
                    spriteWidth,
                    spriteHeight
                );
                target.set(globalIndex, slot, col == 0 && row == 0 ? texture : null);
            }
        }
    }
}
