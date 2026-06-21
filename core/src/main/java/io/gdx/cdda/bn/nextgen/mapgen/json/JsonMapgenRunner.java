package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionalTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.palette.MergedCharMap;
import io.gdx.cdda.bn.nextgen.mapgen.palette.MergedFormatPlacings;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteParser;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.ArrayList;
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
        final Random rng = runOptions.createRng(definition);
        final JsonMapgenRunOptions scopedOptions = scopeParameters(object, runOptions, rng);
        final String fillTer = readFillTer(object, scopedOptions);
        final MergedCharMap merged = buildMergedCharMap(object, palettes, scopedOptions);
        final MergedFormatPlacings formatPlacings = buildFormatPlacings(object, palettes, scopedOptions);

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
                scopedOptions,
                predecessorDepth
            );
        } else if (hasPredecessor) {
            runOptions.addWarning(
                "predecessor_mapgen ignored without MapgenCatalog for " + definition.displayName()
            );
        }

        final int totalTurns = totalRotationTurns(object, scopedOptions);
        if (hasPredecessor && totalTurns != 0) {
            activeGrid = MapGridRotator.rotate(activeGrid, (4 - totalTurns) % 4);
        }
        final MapGrid paintGrid = activeGrid;

        SetmapApplier.apply(paintGrid, object.get("set"), scopedOptions, rng);
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
        PlaceSpawnerApplier.applyTerrainAndFurniture(paintGrid, object, scopedOptions, rng);
        final List<SpawnMarker> collected = new ArrayList<>();
        collected.addAll(FormatPlacingCollector.collectFromRows(rows, formatPlacings, scopedOptions, rng));
        collected.addAll(PlaceSpawnerApplier.collectEntitySpawns(object, paintGrid, scopedOptions, rng));
        final int nestedMarkerStart = runOptions.getSpawnMarkers().size();
        if (catalog != null) {
            NestedMapgenRunner.apply(paintGrid, object, rows, catalog, palettes, scopedOptions, rng, 0);
        }
        collected.addAll(runOptions.drainSpawnMarkersSince(nestedMarkerStart));
        addRotatedSpawnMarkers(
            runOptions,
            collected,
            paintGrid.width(),
            paintGrid.height(),
            totalTurns
        );
        applyRegionalResolve(paintGrid, scopedOptions);
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
        final Random rng = runOptions.createRng(definition);
        final JsonMapgenRunOptions scopedOptions = scopeParameters(object, runOptions, rng);
        final MergedCharMap merged = buildMergedCharMap(object, palettes, scopedOptions);
        final MergedFormatPlacings formatPlacings = buildFormatPlacings(object, palettes, scopedOptions);
        final List<String> rows = RowsInterpreter.readRows(object);

        SetmapApplier.apply(grid, object.get("set"), scopedOptions, rng);
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
        PlaceSpawnerApplier.applyTerrainAndFurniture(grid, object, scopedOptions, rng);
        final List<SpawnMarker> collected = new ArrayList<>();
        collected.addAll(FormatPlacingCollector.collectFromRows(rows, formatPlacings, scopedOptions, rng));
        collected.addAll(PlaceSpawnerApplier.collectEntitySpawns(object, grid, scopedOptions, rng));
        final int nestedMarkerStart = runOptions.getSpawnMarkers().size();
        if (catalog != null) {
            NestedMapgenRunner.apply(grid, object, rows, catalog, palettes, scopedOptions, rng, nestedDepth);
        }
        collected.addAll(runOptions.drainSpawnMarkersSince(nestedMarkerStart));
        runOptions.addSpawnMarkers(collected);
        applyRegionalResolve(grid, scopedOptions);
    }

    private static void addRotatedSpawnMarkers(
        final JsonMapgenRunOptions runOptions,
        final List<SpawnMarker> markers,
        final int gridWidth,
        final int gridHeight,
        final int quarterTurnsClockwise
    ) {
        if (markers == null || markers.isEmpty()) {
            return;
        }
        if (Math.floorMod(quarterTurnsClockwise, 4) == 0) {
            runOptions.addSpawnMarkers(markers);
            return;
        }
        final List<SpawnMarker> rotated = new ArrayList<>(markers.size());
        for (final SpawnMarker marker : markers) {
            final int[] point = MapGridRotator.rotatePointClockwise(
                marker.x,
                marker.y,
                gridWidth,
                gridHeight,
                quarterTurnsClockwise
            );
            rotated.add(
                new SpawnMarker(
                    marker.kind,
                    marker.groupId,
                    marker.displayName,
                    point[0],
                    point[1],
                    marker.density
                )
            );
        }
        runOptions.addSpawnMarkers(rotated);
    }

    private static JsonMapgenRunOptions scopeParameters(
        final JsonValue object,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (object == null || !object.has("parameters")) {
            return options;
        }
        final java.util.Map<String, String> rolled = MapgenParameterRoller.roll(
            object.get("parameters"),
            rng,
            options::addWarning
        );
        if (rolled.isEmpty()) {
            return options;
        }
        return options.withRolledParameters(rolled);
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

    private static MergedFormatPlacings buildFormatPlacings(
        final JsonValue object,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options
    ) {
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final List<String> mergeWarnings = new ArrayList<>();
        final MergedFormatPlacings placings = MergedFormatPlacings.merge(
            palettes,
            RowsInterpreter.readPaletteIds(object),
            object,
            mergeWarnings
        );
        for (final String warning : mergeWarnings) {
            runOptions.addWarning(warning);
        }
        return placings;
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
            runOptions::addWarning,
            runOptions.getRolledParameters()
        );
    }
}
