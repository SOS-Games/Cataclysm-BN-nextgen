package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads {@code rows} strings and measures UTF-32 row width (P2). */
public final class RowsInterpreter {

    private RowsInterpreter() {}

    public static List<String> readRows(final JsonValue objectRoot) {
        if (objectRoot == null || !objectRoot.isObject()) {
            return Collections.emptyList();
        }
        final JsonValue rowsValue = objectRoot.get("rows");
        if (rowsValue == null || !rowsValue.isArray()) {
            return Collections.emptyList();
        }
        final List<String> rows = new ArrayList<>();
        for (JsonValue row = rowsValue.child; row != null; row = row.next) {
            if (row.isString()) {
                rows.add(row.asString());
            }
        }
        return rows;
    }

    public static List<String> readPaletteIds(final JsonValue objectRoot) {
        if (objectRoot == null || !objectRoot.isObject()) {
            return Collections.emptyList();
        }
        final JsonValue palettesValue = objectRoot.get("palettes");
        if (palettesValue == null || !palettesValue.isArray()) {
            return Collections.emptyList();
        }
        final List<String> paletteIds = new ArrayList<>();
        for (JsonValue entry = palettesValue.child; entry != null; entry = entry.next) {
            if (entry.isString()) {
                final String id = entry.asString();
                if (id != null && !id.isEmpty()) {
                    paletteIds.add(id);
                }
            }
        }
        return paletteIds;
    }

    public static int maxRowWidth(final List<String> rows) {
        int max = 0;
        for (final String row : rows) {
            max = Math.max(max, rowWidth(row));
        }
        return max;
    }

    public static int rowWidth(final String row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        return row.codePointCount(0, row.length());
    }

    public static int codePointAtColumn(final String row, final int column) {
        int x = 0;
        for (int i = 0; i < row.length(); ) {
            if (x == column) {
                return row.codePointAt(i);
            }
            i += Character.charCount(row.codePointAt(i));
            x++;
        }
        throw new IndexOutOfBoundsException("column " + column + " out of bounds for row width " + rowWidth(row));
    }
}
