package io.gdx.cdda.bn.nextgen.tileset;

import java.util.Objects;

/** Display entry for a discovered tileset ({@code NAME} + {@code VIEW}). */
public final class TilesetOption {

    private final String id;
    private final String displayName;

    public TilesetOption(final String id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TilesetOption)) {
            return false;
        }
        final TilesetOption that = (TilesetOption) other;
        return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName);
    }

    @Override
    public String toString() {
        return id + " (\"" + displayName + "\")";
    }
}
