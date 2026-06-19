package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;

import java.util.Random;
import java.util.function.BiConsumer;

/** Applies mapgen {@code object.set} entries (P9). */
public final class SetmapApplier {

    private SetmapApplier() {}

    public static void apply(
        final MapGrid grid,
        final JsonValue setArray,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (grid == null || setArray == null || !setArray.isArray() || rng == null) {
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        for (JsonValue entry = setArray.child; entry != null; entry = entry.next) {
            applyEntry(grid, entry, runOptions, rng);
        }
    }

    private static void applyEntry(
        final MapGrid grid,
        final JsonValue entry,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (entry == null || !entry.isObject()) {
            options.addWarning("skipped setmap entry: not an object");
            return;
        }

        final ParsedEntry parsed = parseEntry(entry, options);
        if (parsed == null) {
            return;
        }

        final int chance = entry.getInt("chance", 1);
        if (chance != 1 && rng.nextInt(chance) != 0) {
            return;
        }

        final int repeat = JmapgenIntRange.rollOptional(entry, "repeat", 1, rng);
        for (int i = 0; i < repeat; i++) {
            applyGeometry(grid, entry, parsed, options, rng);
        }
    }

    private static void applyGeometry(
        final MapGrid grid,
        final JsonValue entry,
        final ParsedEntry parsed,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        switch (parsed.geometry) {
            case POINT:
                applyAtCell(grid, rollX(entry, "x", rng), rollY(entry, "y", rng), parsed, options);
                break;
            case LINE:
                applyLine(
                    grid,
                    rollX(entry, "x", rng),
                    rollY(entry, "y", rng),
                    rollX(entry, "x2", rng),
                    rollY(entry, "y2", rng),
                    parsed,
                    options
                );
                break;
            case SQUARE:
                applySquare(
                    grid,
                    rollX(entry, "x", rng),
                    rollY(entry, "y", rng),
                    rollX(entry, "x2", rng),
                    rollY(entry, "y2", rng),
                    parsed,
                    options
                );
                break;
            default:
                break;
        }
    }

    private static int rollX(final JsonValue entry, final String key, final Random rng) {
        return JmapgenIntRange.rollOptional(entry, key, 0, rng);
    }

    private static int rollY(final JsonValue entry, final String key, final Random rng) {
        return JmapgenIntRange.rollOptional(entry, key, 0, rng);
    }

    private static ParsedEntry parseEntry(final JsonValue entry, final JsonMapgenRunOptions options) {
        if (entry.has("point")) {
            return parseOp(entry.getString("point", ""), Geometry.POINT, entry, options);
        }
        if (entry.has("set")) {
            options.addWarning("setmap uses deprecated \"set\" key; use \"point\" instead");
            return parseOp(entry.getString("set", ""), Geometry.POINT, entry, options);
        }
        if (entry.has("line")) {
            return parseOp(entry.getString("line", ""), Geometry.LINE, entry, options);
        }
        if (entry.has("square")) {
            return parseOp(entry.getString("square", ""), Geometry.SQUARE, entry, options);
        }
        options.addWarning("skipped setmap entry: missing point, line, or square");
        return null;
    }

    private static ParsedEntry parseOp(
        final String opName,
        final Geometry geometry,
        final JsonValue entry,
        final JsonMapgenRunOptions options
    ) {
        if (opName == null || opName.isEmpty()) {
            options.addWarning("skipped setmap entry: empty operation");
            return null;
        }
        switch (opName) {
            case "terrain":
                return new ParsedEntry(geometry, Op.TERRAIN, entry.getString("id", ""));
            case "furniture":
                return new ParsedEntry(geometry, Op.FURNITURE, entry.getString("id", ""));
            case "trap":
            case "radiation":
            case "bash":
                options.addWarning("setmap '" + opName + "' not supported in preview");
                return null;
            default:
                options.addWarning("skipped setmap entry: unknown operation '" + opName + "'");
                return null;
        }
    }

    private static void applyAtCell(
        final MapGrid grid,
        final int x,
        final int y,
        final ParsedEntry parsed,
        final JsonMapgenRunOptions options
    ) {
        if (!inBounds(grid, x, y)) {
            options.addWarning("setmap out of bounds at " + x + "," + y);
            return;
        }
        if (parsed.id == null || parsed.id.isEmpty()) {
            options.addWarning("setmap missing id for " + parsed.op);
            return;
        }
        applyOp(grid, x, y, parsed);
    }

    private static void applyLine(
        final MapGrid grid,
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final ParsedEntry parsed,
        final JsonMapgenRunOptions options
    ) {
        if (parsed.id == null || parsed.id.isEmpty()) {
            options.addWarning("setmap line missing id");
            return;
        }
        forEachLineCell(x0, y0, x1, y1, (x, y) -> applyAtCell(grid, x, y, parsed, options));
    }

    private static void applySquare(
        final MapGrid grid,
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final ParsedEntry parsed,
        final JsonMapgenRunOptions options
    ) {
        if (parsed.id == null || parsed.id.isEmpty()) {
            options.addWarning("setmap square missing id");
            return;
        }
        final int minX = Math.min(x0, x1);
        final int maxX = Math.max(x0, x1);
        final int minY = Math.min(y0, y1);
        final int maxY = Math.max(y0, y1);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                applyAtCell(grid, x, y, parsed, options);
            }
        }
    }

    private static void applyOp(final MapGrid grid, final int x, final int y, final ParsedEntry parsed) {
        switch (parsed.op) {
            case TERRAIN:
                grid.setTerrain(x, y, parsed.id);
                break;
            case FURNITURE:
                grid.setFurniture(x, y, parsed.id);
                break;
            default:
                break;
        }
    }

    private static boolean inBounds(final MapGrid grid, final int x, final int y) {
        return x >= 0 && y >= 0 && x < grid.width() && y < grid.height();
    }

    private static void forEachLineCell(
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final BiConsumer<Integer, Integer> cell
    ) {
        int x = x0;
        int y = y0;
        final int dx = Math.abs(x1 - x0);
        final int dy = Math.abs(y1 - y0);
        final int sx = x0 < x1 ? 1 : -1;
        final int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            cell.accept(x, y);
            if (x == x1 && y == y1) {
                break;
            }
            final int err2 = err * 2;
            if (err2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (err2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private enum Geometry {
        POINT,
        LINE,
        SQUARE
    }

    private enum Op {
        TERRAIN,
        FURNITURE
    }

    private static final class ParsedEntry {
        private final Geometry geometry;
        private final Op op;
        private final String id;

        private ParsedEntry(final Geometry geometry, final Op op, final String id) {
            this.geometry = geometry;
            this.op = op;
            this.id = id;
        }
    }
}
