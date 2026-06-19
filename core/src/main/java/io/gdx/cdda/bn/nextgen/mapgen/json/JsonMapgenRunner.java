package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionalTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.palette.MergedCharMap;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteParser;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.List;
import java.util.Random;

/** Applies json mapgen {@code rows} to a {@link MapGrid} (P2). */
public final class JsonMapgenRunner {

    private JsonMapgenRunner() {}

    public static MapGrid run(
        final JsonMapgenDefinition definition,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options
    ) {
        final MapgenCatalog catalog = options == null ? null : options.getMapgenCatalog();
        return run(definition, catalog, palettes, options);
    }

    public static MapGrid run(
        final JsonMapgenDefinition definition,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options
    ) {
        return runInternal(definition, catalog, palettes, options, 0);
    }

    static MapGrid runInternal(
        final JsonMapgenDefinition definition,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final int predecessorDepth
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

        final boolean hasPredecessor = PredecessorMapgenRunner.hasPredecessorMapgen(object);
        int width = PredecessorMapgenRunner.canvasSizeWithPredecessor(RowsInterpreter.maxRowWidth(rows));
        int height = PredecessorMapgenRunner.canvasSizeWithPredecessor(rows.size());
        if (!hasPredecessor) {
            width = RowsInterpreter.maxRowWidth(rows);
            height = rows.size();
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("mapgen rows produced empty grid: " + definition.displayName());
        }

        final MapGrid grid = new MapGrid(width, height, fillTer);
        MapGrid activeGrid = grid;
        if (hasPredecessor && catalog != null) {
            PredecessorMapgenRunner.paintPredecessor(
                activeGrid,
                object,
                definition,
                catalog,
                palettes,
                runOptions,
                predecessorDepth
            );
        } else if (hasPredecessor) {
            runOptions.addWarning(
                "predecessor_mapgen ignored without MapgenCatalog for " + definition.displayName()
            );
        }

        final int totalTurns = totalRotationTurns(object, runOptions);
        if (hasPredecessor && totalTurns != 0) {
            activeGrid = MapGridRotator.rotate(activeGrid, (4 - totalTurns) % 4);
        }
        final MapGrid paintGrid = activeGrid;

        final Random rng = runOptions.createRng(definition);
        SetmapApplier.apply(paintGrid, object.get("set"), runOptions, rng);
        for (int y = 0; y < rows.size(); y++) {
            final String row = rows.get(y);
            final int rowWidth = RowsInterpreter.rowWidth(row);
            for (int x = 0; x < rowWidth; x++) {
                final int codePoint = RowsInterpreter.codePointAtColumn(row, x);
                final int cellX = x;
                final int cellY = y;
                merged.terrainForCodePoint(codePoint).ifPresent(ter -> paintGrid.setTerrain(cellX, cellY, ter));
                merged.furnitureForCodePoint(codePoint).ifPresent(furn -> paintGrid.setFurniture(cellX, cellY, furn));
            }
        }
        PlaceSpawnerApplier.applyTerrainAndFurniture(paintGrid, object, runOptions, rng);
        PlaceSpawnerApplier.collectEntitySpawns(object, paintGrid, runOptions, rng);
        if (catalog != null) {
            NestedMapgenRunner.apply(paintGrid, object, rows, catalog, palettes, runOptions, rng, 0);
        }
        applyRegionalResolve(paintGrid, runOptions);
        return MapGridRotator.rotate(paintGrid, totalTurns);
    }

    static void runOverlayOnto(
        final MapGrid grid,
        final JsonMapgenDefinition definition,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final int nestedDepth
    ) {
        if (grid == null || definition == null || palettes == null) {
            return;
        }
        if (!definition.isJsonPreviewSupported()) {
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final JsonValue object = definition.getObjectRoot();
        final MergedCharMap merged = buildMergedCharMap(object, palettes, runOptions);
        final List<String> rows = RowsInterpreter.readRows(object);
        final Random rng = runOptions.createRng(definition);

        SetmapApplier.apply(grid, object.get("set"), runOptions, rng);
        for (int y = 0; y < rows.size(); y++) {
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
        PlaceSpawnerApplier.applyTerrainAndFurniture(grid, object, runOptions, rng);
        PlaceSpawnerApplier.collectEntitySpawns(object, grid, runOptions, rng);
        if (catalog != null) {
            NestedMapgenRunner.apply(grid, object, rows, catalog, palettes, runOptions, rng, nestedDepth);
        }
        applyRegionalResolve(grid, runOptions);
    }

    private static int totalRotationTurns(final JsonValue object, final JsonMapgenRunOptions options) {
        int turns = object == null ? 0 : object.getInt("rotation", 0);
        turns += options == null ? 0 : options.getOmtRotation();
        return Math.floorMod(turns, 4);
    }

    private static void applyRegionalResolve(final MapGrid grid, final JsonMapgenRunOptions runOptions) {
        final RegionContext regionContext = runOptions.getRegionContext();
        if (regionContext != null) {
            RegionalTerrainResolver.applyToGrid(grid, regionContext, runOptions);
        }
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
        final MergedCharMap merged = palettes.merge(paletteIds, mergeWarnings, options);
        for (final String warning : mergeWarnings) {
            options.addWarning(warning);
        }
        applyInlineOverrides(merged, object.get("terrain"), object.get("furniture"), options);
        return merged;
    }

    private static void applyInlineOverrides(
        final MergedCharMap merged,
        final JsonValue terrainOverrides,
        final JsonValue furnitureOverrides,
        final JsonMapgenRunOptions options
    ) {
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        PaletteRegistry.applyInlineNodes(
            merged,
            PaletteParser.parseCharSectionNodes(terrainOverrides),
            PaletteParser.parseCharSectionNodes(furnitureOverrides),
            runOptions.paletteRng(),
            runOptions::addWarning
        );
    }
}
