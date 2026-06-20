package io.gdx.cdda.bn.nextgen.worldgen.overmap;

/** Row-major OMT id grid for mini-overmap preview (W2). */
public final class OvermapGrid {

    private final int width;
    private final int height;
    private final String[] cells;

    public OvermapGrid(final int width, final int height, final String fillId) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        if (fillId == null || fillId.isEmpty()) {
            throw new IllegalArgumentException("fillId is required");
        }
        this.width = width;
        this.height = height;
        this.cells = new String[width * height];
        fill(fillId);
    }

    private OvermapGrid(final int width, final int height, final String[] cells) {
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String getOmtId(final int omtX, final int omtY) {
        return cells[indexOf(omtX, omtY)];
    }

    public void setOmtId(final int omtX, final int omtY, final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("omt id is required");
        }
        cells[indexOf(omtX, omtY)] = id;
    }

    public void fill(final String id) {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = id;
        }
    }

    private int indexOf(final int omtX, final int omtY) {
        if (omtX < 0 || omtY < 0 || omtX >= width || omtY >= height) {
            throw new IndexOutOfBoundsException("(" + omtX + "," + omtY + ") outside " + width + "x" + height);
        }
        return omtY * width + omtX;
    }

    static OvermapGrid fromCells(final int width, final int height, final String[] cells) {
        if (cells.length != width * height) {
            throw new IllegalArgumentException("cell count mismatch");
        }
        return new OvermapGrid(width, height, cells.clone());
    }
}
