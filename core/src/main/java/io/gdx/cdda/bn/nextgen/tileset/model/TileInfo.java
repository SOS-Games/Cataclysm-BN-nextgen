package io.gdx.cdda.bn.nextgen.tileset.model;

/** Parsed {@code tile_info[0]} fields (unit 04b). */
public final class TileInfo {

    private final int width;
    private final int height;
    private final float pixelScale;
    private final boolean iso;

    public TileInfo(final int width, final int height, final float pixelScale, final boolean iso) {
        this.width = width;
        this.height = height;
        this.pixelScale = pixelScale;
        this.iso = iso;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getPixelScale() {
        return pixelScale;
    }

    public boolean isIso() {
        return iso;
    }
}
