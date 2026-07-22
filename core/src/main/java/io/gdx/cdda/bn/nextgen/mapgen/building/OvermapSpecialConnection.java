package io.gdx.cdda.bn.nextgen.mapgen.building;

/**
 * One {@code overmap_special} road/tunnel connection stub (BN {@code overmap_special_connection}).
 */
public final class OvermapSpecialConnection {

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final Integer fromX;
    private final Integer fromY;
    private final Integer fromZ;
    private final String connectionId;
    private final boolean existing;

    public OvermapSpecialConnection(
        final int offsetX,
        final int offsetY,
        final int offsetZ,
        final Integer fromX,
        final Integer fromY,
        final Integer fromZ,
        final String connectionId,
        final boolean existing
    ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.connectionId = connectionId == null ? "" : connectionId;
        this.existing = existing;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public boolean hasFrom() {
        return fromX != null && fromY != null && fromZ != null;
    }

    public Integer getFromX() {
        return fromX;
    }

    public Integer getFromY() {
        return fromY;
    }

    public Integer getFromZ() {
        return fromZ;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public boolean isExisting() {
        return existing;
    }

    /** Cardinal 0=N..3=W from {@code from} → {@code point}, or -1 if unknown. */
    public int initialDir() {
        if (!hasFrom()) {
            return -1;
        }
        final int dx = offsetX - fromX;
        final int dy = offsetY - fromY;
        if (dx == 0 && dy == -1) {
            return 0;
        }
        if (dx == 1 && dy == 0) {
            return 1;
        }
        if (dx == 0 && dy == 1) {
            return 2;
        }
        if (dx == -1 && dy == 0) {
            return 3;
        }
        return -1;
    }
}
