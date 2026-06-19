package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapCell;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Applies parent {@code place_nested} / format {@code nested} chunks (P15). */
public final class NestedMapgenRunner {

    static final int MAX_DEPTH = 4;
    private static final int DEFAULT_MAPGENSIZE = 24;

    private NestedMapgenRunner() {}

    public static void apply(
        final MapGrid parent,
        final JsonValue object,
        final List<String> rows,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final Random rng,
        final int depth
    ) {
        if (parent == null || object == null || catalog == null || palettes == null || rng == null) {
            return;
        }
        if (depth >= MAX_DEPTH) {
            options.addWarning("nested mapgen depth limit (" + MAX_DEPTH + ") exceeded");
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;

        final JsonValue placeNested = object.get("place_nested");
        if (placeNested != null && placeNested.isArray()) {
            applyPlacementArray(parent, placeNested, catalog, palettes, runOptions, rng, depth);
        }

        final JsonValue nested = object.get("nested");
        if (nested != null && nested.isArray()) {
            applyPlacementArray(parent, nested, catalog, palettes, runOptions, rng, depth);
        } else if (nested != null && nested.isObject() && rows != null && !rows.isEmpty()) {
            applyFormatNested(parent, nested, rows, catalog, palettes, runOptions, rng, depth);
        }
    }

    private static void applyPlacementArray(
        final MapGrid parent,
        final JsonValue array,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final Random rng,
        final int depth
    ) {
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            if (entry == null || !entry.isObject()) {
                options.addWarning("skipped nested entry: not an object");
                continue;
            }
            final int destX = JmapgenIntRange.rollOptional(entry, "x", 0, rng);
            final int destY = JmapgenIntRange.rollOptional(entry, "y", 0, rng);
            applyNestedEntry(parent, entry, destX, destY, catalog, palettes, options, rng, depth);
        }
    }

    private static void applyFormatNested(
        final MapGrid parent,
        final JsonValue nestedByChar,
        final List<String> rows,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final Random rng,
        final int depth
    ) {
        for (int y = 0; y < rows.size(); y++) {
            final String row = rows.get(y);
            final int rowWidth = RowsInterpreter.rowWidth(row);
            for (int x = 0; x < rowWidth; x++) {
                final int codePoint = RowsInterpreter.codePointAtColumn(row, x);
                final String key = new String(Character.toChars(codePoint));
                final JsonValue entry = nestedByChar.get(key);
                if (entry == null || !entry.isObject()) {
                    continue;
                }
                applyNestedEntry(parent, entry, x, y, catalog, palettes, options, rng, depth);
            }
        }
    }

    private static void applyNestedEntry(
        final MapGrid parent,
        final JsonValue entry,
        final int destX,
        final int destY,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final Random rng,
        final int depth
    ) {
        final Optional<ResolvedChunk> chunk = pickChunk(entry.get("chunks"), catalog, options, rng);
        if (chunk.isEmpty()) {
            return;
        }
        if (chunk.get().skip) {
            return;
        }

        final JsonMapgenDefinition definition = chunk.get().definition;
        final int chunkWidth = readMapgensizeWidth(definition.getObjectRoot());
        final int chunkHeight = readMapgensizeHeight(definition.getObjectRoot());
        if (destX + chunkWidth > parent.width() || destY + chunkHeight > parent.height()) {
            options.addWarning(
                "nested chunk '" + definition.displayName() + "' at " + destX + "," + destY
                    + " extends past parent bounds"
            );
        }

        final MapGrid overlay = copyParentRegion(parent, destX, destY, chunkWidth, chunkHeight);
        JsonMapgenRunner.runOverlayOnto(overlay, definition, catalog, palettes, options, depth + 1);
        parent.blitFrom(overlay, destX, destY, null);
    }

    static MapGrid copyParentRegion(
        final MapGrid parent,
        final int destX,
        final int destY,
        final int width,
        final int height
    ) {
        final MapGrid region = new MapGrid(width, height, parent.getDefaultTerrainId());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int parentX = destX + x;
                final int parentY = destY + y;
                if (parentX < 0 || parentY < 0 || parentX >= parent.width() || parentY >= parent.height()) {
                    continue;
                }
                final MapCell cell = parent.get(parentX, parentY);
                region.setTerrain(x, y, cell.getTerrainId());
                final String furnitureId = cell.getFurnitureId();
                if (furnitureId != null && !furnitureId.isEmpty()) {
                    region.setFurniture(x, y, furnitureId);
                }
            }
        }
        return region;
    }

    static int readMapgensizeWidth(final JsonValue object) {
        return readMapgensize(object).width;
    }

    static int readMapgensizeHeight(final JsonValue object) {
        return readMapgensize(object).height;
    }

    static Mapgensize readMapgensize(final JsonValue object) {
        if (object != null && object.has("mapgensize")) {
            final JsonValue size = object.get("mapgensize");
            if (size != null && size.isArray() && size.size >= 2) {
                final int width = Math.max(1, size.getInt(0));
                final int height = Math.max(1, size.getInt(1));
                return new Mapgensize(width, height);
            }
        }
        final List<String> rows = RowsInterpreter.readRows(object);
        if (!rows.isEmpty()) {
            return new Mapgensize(RowsInterpreter.maxRowWidth(rows), rows.size());
        }
        return new Mapgensize(DEFAULT_MAPGENSIZE, DEFAULT_MAPGENSIZE);
    }

    private static Optional<ResolvedChunk> pickChunk(
        final JsonValue chunks,
        final MapgenCatalog catalog,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (chunks == null) {
            options.addWarning("skipped nested entry: missing chunks");
            return Optional.empty();
        }
        final Optional<ChunkChoice> choice = pickChunkChoice(chunks, options, rng);
        if (choice.isEmpty()) {
            return Optional.empty();
        }
        if (choice.get().skip) {
            return Optional.of(ResolvedChunk.skip());
        }
        if (choice.get().inlineObject != null) {
            return Optional.of(ResolvedChunk.of(inlineDefinition(choice.get().inlineObject)));
        }
        if (catalog == null) {
            options.addWarning("nested chunk '" + choice.get().chunkId + "' requires MapgenCatalog");
            return Optional.empty();
        }
        final Optional<JsonMapgenDefinition> definition = catalog.pickNestedMapgen(choice.get().chunkId, rng);
        if (definition.isEmpty()) {
            options.addWarning("unknown nested mapgen: " + choice.get().chunkId);
            return Optional.empty();
        }
        if (!definition.get().isJsonPreviewSupported()) {
            options.addWarning("unsupported nested mapgen: " + choice.get().chunkId);
            return Optional.empty();
        }
        return Optional.of(ResolvedChunk.of(definition.get()));
    }

    private static JsonMapgenDefinition inlineDefinition(final JsonValue inlineObject) {
        return new JsonMapgenDefinition(
            List.of("inline_nested"),
            null,
            "inline_nested",
            null,
            "json",
            1000,
            false,
            Paths.get("inline_nested.json"),
            0,
            inlineObject
        );
    }

    private static Optional<ChunkChoice> pickChunkChoice(
        final JsonValue chunks,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        final List<ChunkChoice> choices = parseChunkChoices(chunks, options);
        if (choices.isEmpty()) {
            options.addWarning("skipped nested entry: empty chunks");
            return Optional.empty();
        }
        if (choices.size() == 1) {
            return Optional.of(choices.get(0));
        }
        int totalWeight = 0;
        for (final ChunkChoice choice : choices) {
            totalWeight += choice.weight;
        }
        int roll = rng.nextInt(totalWeight);
        for (final ChunkChoice choice : choices) {
            roll -= choice.weight;
            if (roll < 0) {
                return Optional.of(choice);
            }
        }
        return Optional.of(choices.get(choices.size() - 1));
    }

    private static List<ChunkChoice> parseChunkChoices(
        final JsonValue chunks,
        final JsonMapgenRunOptions options
    ) {
        final List<ChunkChoice> choices = new ArrayList<>();
        if (chunks.isString()) {
            return parseChunkToken(chunks, 1, options);
        }
        if (!chunks.isArray()) {
            options.addWarning("skipped nested entry: chunks is not an array");
            return choices;
        }
        for (JsonValue child = chunks.child; child != null; child = child.next) {
            if (child.isString()) {
                choices.addAll(parseChunkToken(child, 1, options));
                continue;
            }
            if (child.isObject()) {
                if (child.has("object") && child.get("object").isObject()) {
                    choices.add(new ChunkChoice(null, child.get("object"), 1, false));
                } else {
                    options.addWarning("skipped nested inline chunk: missing object");
                }
                continue;
            }
            if (!child.isArray()) {
                continue;
            }
            if (child.size >= 2 && child.get(1).isNumber()) {
                final JsonValue idNode = child.get(0);
                if (idNode != null && idNode.isString()) {
                    choices.addAll(parseChunkToken(idNode, Math.max(1, child.getInt(1)), options));
                }
                continue;
            }
            for (JsonValue inner = child.child; inner != null; inner = inner.next) {
                if (inner.isString()) {
                    choices.addAll(parseChunkToken(inner, 1, options));
                }
            }
        }
        return choices;
    }

    private static List<ChunkChoice> parseChunkToken(
        final JsonValue token,
        final int weight,
        final JsonMapgenRunOptions options
    ) {
        final String id = token.asString();
        if (id == null || id.isEmpty()) {
            return List.of();
        }
        if ("null".equals(id)) {
            return List.of(new ChunkChoice(null, null, weight, true));
        }
        return List.of(new ChunkChoice(id, null, weight, false));
    }

    static final class Mapgensize {
        final int width;
        final int height;

        Mapgensize(final int width, final int height) {
            this.width = width;
            this.height = height;
        }
    }

    private static final class ChunkChoice {
        private final String chunkId;
        private final JsonValue inlineObject;
        private final int weight;
        private final boolean skip;

        private ChunkChoice(
            final String chunkId,
            final JsonValue inlineObject,
            final int weight,
            final boolean skip
        ) {
            this.chunkId = chunkId;
            this.inlineObject = inlineObject;
            this.weight = weight;
            this.skip = skip;
        }
    }

    private static final class ResolvedChunk {
        private final JsonMapgenDefinition definition;
        private final boolean skip;

        private ResolvedChunk(final JsonMapgenDefinition definition, final boolean skip) {
            this.definition = definition;
            this.skip = skip;
        }

        private static ResolvedChunk of(final JsonMapgenDefinition definition) {
            return new ResolvedChunk(definition, false);
        }

        private static ResolvedChunk skip() {
            return new ResolvedChunk(null, true);
        }
    }
}
