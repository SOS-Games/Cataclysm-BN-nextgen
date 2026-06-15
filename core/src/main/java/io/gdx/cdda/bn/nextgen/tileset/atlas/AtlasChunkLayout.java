package io.gdx.cdda.bn.nextgen.tileset.atlas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Upload-chunk grid for large atlases (unit 06b). */
public final class AtlasChunkLayout {

    private AtlasChunkLayout() {}

    public static final class ChunkRect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public ChunkRect(final int x, final int y, final int width, final int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public static List<ChunkRect> computeChunks(
        final int atlasWidth,
        final int atlasHeight,
        final int spriteWidth,
        final int spriteHeight,
        final int maxTextureWidth,
        final int maxTextureHeight
    ) {
        if (spriteWidth <= 0 || spriteHeight <= 0) {
            throw new IllegalArgumentException("sprite dimensions must be positive");
        }
        if (maxTextureWidth < spriteWidth || maxTextureHeight < spriteHeight) {
            throw new IllegalArgumentException(
                "max texture size must be at least one sprite cell ("
                    + maxTextureWidth + "x" + maxTextureHeight + " vs "
                    + spriteWidth + "x" + spriteHeight + ")"
            );
        }

        final int maxTileXCount = maxTextureWidth / spriteWidth;
        final int maxTileYCount = maxTextureHeight / spriteHeight;
        final int chunkPixelWidth = maxTileXCount * spriteWidth;
        final int chunkPixelHeight = maxTileYCount * spriteHeight;
        final int chunksX = AtlasGrid.divideRoundUp(atlasWidth, maxTextureWidth);
        final int chunksY = AtlasGrid.divideRoundUp(atlasHeight, maxTextureHeight);

        final List<ChunkRect> chunks = new ArrayList<>(chunksX * chunksY);
        for (int chunkY = 0; chunkY < chunksY; chunkY++) {
            for (int chunkX = 0; chunkX < chunksX; chunkX++) {
                final int x = chunkX * chunkPixelWidth;
                final int y = chunkY * chunkPixelHeight;
                final int width = Math.min(atlasWidth - x, chunkPixelWidth);
                final int height = Math.min(atlasHeight - y, chunkPixelHeight);
                if (width > 0 && height > 0) {
                    chunks.add(new ChunkRect(x, y, width, height));
                }
            }
        }
        return Collections.unmodifiableList(chunks);
    }

    public static int resolveMaxTextureWidth(
        final int requestedMax,
        final int spriteWidth,
        final int minTileXCount
    ) {
        if (requestedMax <= 0) {
            return spriteWidth * minTileXCount;
        }
        return requestedMax;
    }

    public static int resolveMaxTextureHeight(
        final int requestedMax,
        final int spriteHeight,
        final int minTileYCount
    ) {
        if (requestedMax <= 0) {
            return spriteHeight * minTileYCount;
        }
        return requestedMax;
    }
}
