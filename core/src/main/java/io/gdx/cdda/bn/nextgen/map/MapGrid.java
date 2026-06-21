package io.gdx.cdda.bn.nextgen.map;

/** Mutable 2D grid for map editor terrain/furniture layers. */
public final class MapGrid {

    private int width;
    private int height;
    private String defaultTerrainId;
    private MapCell[] cells;

    public MapGrid(final int width, final int height, final String fillTerrainId) {
        validateDimensions(width, height);
        validateTerrainId(fillTerrainId);
        this.width = width;
        this.height = height;
        this.defaultTerrainId = fillTerrainId;
        this.cells = new MapCell[width * height];
        fill(fillTerrainId);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String getDefaultTerrainId() {
        return defaultTerrainId;
    }

    public void setDefaultTerrainId(final String defaultTerrainId) {
        validateTerrainId(defaultTerrainId);
        this.defaultTerrainId = defaultTerrainId;
    }

    public MapCell get(final int x, final int y) {
        return cells[indexOf(x, y)];
    }

    public void setTerrain(final int x, final int y, final String terrainId) {
        validateTerrainId(terrainId);
        get(x, y).setTerrainId(terrainId);
    }

    public void setFurniture(final int x, final int y, final String furnitureId) {
        get(x, y).setFurnitureId(furnitureId);
    }

    public void clearFurniture(final int x, final int y) {
        get(x, y).setFurnitureId(null);
    }

    public void fill(final String terrainId) {
        validateTerrainId(terrainId);
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new MapCell(terrainId, null);
        }
    }

    /** Copies terrain and furniture from {@code source} into this grid. Returns overlap count. */
    public int blitFrom(
        final MapGrid source,
        final int destX,
        final int destY,
        final String unsetFillTer
    ) {
        return blitFrom(source, 0, 0, source.width(), source.height(), destX, destY, unsetFillTer);
    }

    /** Copies a sub-rectangle from {@code source} into this grid. Returns overlap count. */
    public int blitFrom(
        final MapGrid source,
        final int sourceX,
        final int sourceY,
        final int copyWidth,
        final int copyHeight,
        final int destX,
        final int destY,
        final String unsetFillTer
    ) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (copyWidth <= 0 || copyHeight <= 0) {
            throw new IllegalArgumentException("copyWidth and copyHeight must be > 0");
        }
        int overlaps = 0;
        for (int y = 0; y < copyHeight; y++) {
            for (int x = 0; x < copyWidth; x++) {
                final int srcX = sourceX + x;
                final int srcY = sourceY + y;
                if (srcX < 0 || srcY < 0 || srcX >= source.width() || srcY >= source.height()) {
                    continue;
                }
                final int targetX = destX + x;
                final int targetY = destY + y;
                if (targetX < 0 || targetY < 0 || targetX >= width || targetY >= height) {
                    continue;
                }
                final MapCell src = source.get(srcX, srcY);
                final MapCell dest = get(targetX, targetY);
                if (!isUnsetCell(dest, unsetFillTer) && contentDiffers(dest, src)) {
                    overlaps++;
                }
                dest.setTerrainId(src.getTerrainId());
                dest.setFurnitureId(src.getFurnitureId());
            }
        }
        return overlaps;
    }

    public void resize(final int newWidth, final int newHeight, final String fillTerrainId) {
        validateDimensions(newWidth, newHeight);
        validateTerrainId(fillTerrainId);

        final MapCell[] resized = new MapCell[newWidth * newHeight];
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                final int newIndex = y * newWidth + x;
                if (x < width && y < height) {
                    final MapCell old = cells[indexOf(x, y)];
                    resized[newIndex] = new MapCell(old.getTerrainId(), old.getFurnitureId());
                } else {
                    resized[newIndex] = new MapCell(fillTerrainId, null);
                }
            }
        }

        this.width = newWidth;
        this.height = newHeight;
        this.defaultTerrainId = fillTerrainId;
        this.cells = resized;
    }

    private int indexOf(final int x, final int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IndexOutOfBoundsException(
                "Coordinates out of bounds: (" + x + "," + y + ") for " + width + "x" + height
            );
        }
        return y * width + x;
    }

    private static void validateDimensions(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
    }

    private static boolean contentDiffers(final MapCell dest, final MapCell src) {
        return !dest.getTerrainId().equals(src.getTerrainId())
            || !furnitureEquals(dest.getFurnitureId(), src.getFurnitureId());
    }

    private static boolean furnitureEquals(final String left, final String right) {
        final String a = left == null || left.isEmpty() ? null : left;
        final String b = right == null || right.isEmpty() ? null : right;
        if (a == null) {
            return b != null;
        }
        return !a.equals(b);
    }

    private static boolean isUnsetCell(final MapCell cell, final String unsetFillTer) {
        final String furnitureId = cell.getFurnitureId();
        return cell.getTerrainId().equals(unsetFillTer)
            && (furnitureId == null || furnitureId.isEmpty());
    }

    private static void validateTerrainId(final String terrainId) {
        if (terrainId == null || terrainId.trim().isEmpty()) {
            throw new IllegalArgumentException("terrain id must be non-empty");
        }
    }
}
