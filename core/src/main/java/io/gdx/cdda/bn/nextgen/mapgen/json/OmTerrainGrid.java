package io.gdx.cdda.bn.nextgen.mapgen.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 2D {@code om_terrain} grid from BN multitile json mapgen entries. */
public final class OmTerrainGrid {

    private final List<List<String>> rows;

    public OmTerrainGrid(final List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("rows must not be empty");
        }
        final int width = rows.get(0).size();
        if (width == 0) {
            throw new IllegalArgumentException("rows must not be empty");
        }
        final List<List<String>> copy = new ArrayList<>();
        for (final List<String> row : rows) {
            if (row.size() != width) {
                throw new IllegalArgumentException("om_terrain grid rows must be uniform width");
            }
            copy.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        this.rows = Collections.unmodifiableList(copy);
    }

    public int height() {
        return rows.size();
    }

    public int width() {
        return rows.get(0).size();
    }

    public String get(final int row, final int col) {
        return rows.get(row).get(col);
    }

    public List<String> flatten() {
        final List<String> flat = new ArrayList<>();
        for (final List<String> row : rows) {
            flat.addAll(row);
        }
        return flat;
    }
}
