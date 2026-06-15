package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;

import io.gdx.cdda.bn.nextgen.tileset.model.TileLookup;

/** Uploads sheet cells into a dynamic atlas with content-hash dedup (A1). */
public final class DynamicSheetUploader {

    private DynamicSheetUploader() {}

    public static int uploadSheet(
        final Pixmap atlasPixmap,
        final int spriteWidth,
        final int spriteHeight,
        final int globalOffset,
        final DynamicAtlas atlas,
        final TileLookup tileLookup
    ) {
        final int atlasWidth = atlasPixmap.getWidth();
        final int atlasHeight = atlasPixmap.getHeight();
        final int expectedTileCount = AtlasGrid.expectedTileCount(
            atlasWidth, atlasHeight, spriteWidth, spriteHeight
        );
        final int columns = AtlasGrid.columns(atlasWidth, spriteWidth);
        final int rows = AtlasGrid.rows(atlasHeight, spriteHeight);

        final Pixmap staging = atlas.getStagingPixmap(spriteWidth, spriteHeight);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                final int pixelX = col * spriteWidth;
                final int pixelY = row * spriteHeight;
                final int globalIndex = AtlasGrid.globalIndex(
                    pixelX,
                    pixelY,
                    atlasWidth,
                    spriteWidth,
                    spriteHeight,
                    globalOffset
                );

                staging.drawPixmap(atlasPixmap, 0, 0, pixelX, pixelY, spriteWidth, spriteHeight);
                final long contentHash = PixmapContentHash.hash(staging);
                final AtlasPlacement placement = atlas.getOrCreateSprite(
                    spriteWidth,
                    spriteHeight,
                    contentHash,
                    (destination, destX, destY, width, height) ->
                        destination.drawPixmap(staging, destX, destY, 0, 0, width, height)
                );
                tileLookup.putBase(globalIndex, placement);
            }
        }
        return expectedTileCount;
    }
}
