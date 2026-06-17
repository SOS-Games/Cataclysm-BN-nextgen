package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.palette.MergedCharMap;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteParser;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.List;

/** Applies json mapgen {@code rows} to a {@link MapGrid} (P2). */
public final class JsonMapgenRunner {

    private JsonMapgenRunner() {}

    public static MapGrid run(
        final JsonMapgenDefinition definition,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options
    ) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        if (!definition.isJsonPreviewSupported()) {
            throw new IllegalArgumentException("mapgen is not runnable: " + definition.displayName());
        }
        if (palettes == null) {
            throw new IllegalArgumentException("palettes registry is required");
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;

        final JsonValue object = definition.getObjectRoot();
        final String fillTer = readFillTer(object, runOptions);
        final MergedCharMap merged = buildMergedCharMap(object, palettes, runOptions);

        final List<String> rows = RowsInterpreter.readRows(object);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("mapgen object has no rows: " + definition.displayName());
        }

        final int width = RowsInterpreter.maxRowWidth(rows);
        final int height = rows.size();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("mapgen rows produced empty grid: " + definition.displayName());
        }

        final MapGrid grid = new MapGrid(width, height, fillTer);
        for (int y = 0; y < height; y++) {
            final String row = rows.get(y);
            final int rowWidth = RowsInterpreter.rowWidth(row);
            for (int x = 0; x < rowWidth; x++) {
                final int codePoint = RowsInterpreter.codePointAtColumn(row, x);
                final int cellX = x;
                final int cellY = y;
                merged.terrainForCodePoint(codePoint).ifPresent(ter -> grid.setTerrain(cellX, cellY, ter));
                merged.furnitureForCodePoint(codePoint).ifPresent(furn -> grid.setFurniture(cellX, cellY, furn));
            }
        }
        return grid;
    }

    private static String readFillTer(final JsonValue object, final JsonMapgenRunOptions options) {
        final String fillTer = object.getString("fill_ter", null);
        if (fillTer != null && !fillTer.isEmpty()) {
            return fillTer;
        }
        return options.getDefaultFillTer();
    }

    private static MergedCharMap buildMergedCharMap(
        final JsonValue object,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options
    ) {
        final List<String> paletteIds = RowsInterpreter.readPaletteIds(object);
        final List<String> mergeWarnings = new java.util.ArrayList<>();
        final MergedCharMap merged = palettes.merge(paletteIds, mergeWarnings);
        for (final String warning : mergeWarnings) {
            options.addWarning(warning);
        }
        applyInlineOverrides(merged, object.get("terrain"), object.get("furniture"));
        return merged;
    }

    private static void applyInlineOverrides(
        final MergedCharMap merged,
        final JsonValue terrainOverrides,
        final JsonValue furnitureOverrides
    ) {
        merged.putAllTerrain(PaletteParser.parseCharSection(terrainOverrides));
        merged.putAllFurniture(PaletteParser.parseCharSection(furnitureOverrides));
    }
}
