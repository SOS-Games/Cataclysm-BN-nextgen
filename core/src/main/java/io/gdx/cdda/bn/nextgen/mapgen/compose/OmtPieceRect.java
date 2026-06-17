package io.gdx.cdda.bn.nextgen.mapgen.compose;

/** Cell-space bounds of one stitched OMT piece on a floor grid (P6). */
public final class OmtPieceRect {

    private final int originX;
    private final int originY;
    private final int width;
    private final int height;
    private final String overmapId;

    public OmtPieceRect(
        final int originX,
        final int originY,
        final int width,
        final int height,
        final String overmapId
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
        this.overmapId = overmapId == null ? "" : overmapId;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getOvermapId() {
        return overmapId;
    }
}
