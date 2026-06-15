package io.gdx.cdda.bn.nextgen.tileset.atlas;

/** Sprite grid math for a sheet atlas (unit 06a). */
public final class AtlasGrid {

    private AtlasGrid() {}

    public static int columns(final int atlasWidth, final int spriteWidth) {
        return atlasWidth / spriteWidth;
    }

    public static int rows(final int atlasHeight, final int spriteHeight) {
        return atlasHeight / spriteHeight;
    }

    public static int expectedTileCount(
        final int atlasWidth,
        final int atlasHeight,
        final int spriteWidth,
        final int spriteHeight
    ) {
        return columns(atlasWidth, spriteWidth) * rows(atlasHeight, spriteHeight);
    }

    public static int localIndex(
        final int pixelX,
        final int pixelY,
        final int atlasWidth,
        final int spriteWidth,
        final int spriteHeight
    ) {
        final int col = pixelX / spriteWidth;
        final int row = pixelY / spriteHeight;
        return col + row * columns(atlasWidth, spriteWidth);
    }

    public static int globalIndex(
        final int pixelX,
        final int pixelY,
        final int atlasWidth,
        final int spriteWidth,
        final int spriteHeight,
        final int offset
    ) {
        return offset + localIndex(pixelX, pixelY, atlasWidth, spriteWidth, spriteHeight);
    }

    public static int divideRoundUp(final int numerator, final int divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor must be positive");
        }
        return (numerator + divisor - 1) / divisor;
    }
}
