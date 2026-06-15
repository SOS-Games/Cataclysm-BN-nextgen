package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;

/** Content hash for dynamic-atlas sprite dedup (A1 / BN {@code get_surface_hash}). */
public final class PixmapContentHash {

    private PixmapContentHash() {}

    public static long hash(final Pixmap pixmap) {
        return hash(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight());
    }

    public static long hash(
        final Pixmap pixmap,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        long hash = 0L;
        hash = combine(hash, width);
        hash = combine(hash, height);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                hash = combine(hash, pixmap.getPixel(x + col, y + row));
            }
        }
        return hash;
    }

    private static long combine(final long seed, final int value) {
        return seed ^ (Long.rotateLeft(seed, 5) + value * 0x9e3779b97f4a7c15L);
    }
}
