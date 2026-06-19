package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Parses {@code type: mapgen} JSON objects (P2). */
public final class JsonMapgenParser {

    private JsonMapgenParser() {}

    public static Optional<JsonMapgenDefinition> parse(
        final JsonValue root,
        final Path sourceFile,
        final int indexInFile
    ) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        if (!"mapgen".equals(root.getString("type", ""))) {
            return Optional.empty();
        }

        final JsonValue objectRoot = root.get("object");
        if (objectRoot == null || !objectRoot.isObject()) {
            return Optional.empty();
        }

        final String nestedMapgenId = root.getString("nested_mapgen_id", null);
        final String updateMapgenId = root.getString("update_mapgen_id", null);
        final OmTerrainParseResult omTerrain = parseOmTerrain(root.get("om_terrain"));
        if ((nestedMapgenId == null || nestedMapgenId.isEmpty())
            && (updateMapgenId == null || updateMapgenId.isEmpty())
            && omTerrain.flatIds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new JsonMapgenDefinition(
            omTerrain.flatIds,
            omTerrain.grid.orElse(null),
            nestedMapgenId,
            updateMapgenId,
            root.getString("method", ""),
            root.getInt("weight", 1000),
            root.getBoolean("disabled", false),
            sourceFile,
            indexInFile,
            objectRoot
        ));
    }

    private static OmTerrainParseResult parseOmTerrain(final JsonValue value) {
        final List<String> flatIds = new ArrayList<>();
        if (value == null) {
            return new OmTerrainParseResult(flatIds, Optional.empty());
        }
        if (value.isString()) {
            final String id = value.asString();
            if (id != null && !id.isEmpty()) {
                flatIds.add(id);
            }
            return new OmTerrainParseResult(flatIds, Optional.empty());
        }
        if (!value.isArray()) {
            return new OmTerrainParseResult(flatIds, Optional.empty());
        }

        final Optional<OmTerrainGrid> grid = tryParseStringGrid(value);
        if (grid.isPresent()) {
            flatIds.addAll(grid.get().flatten());
            return new OmTerrainParseResult(flatIds, grid);
        }

        flattenOmTerrainArray(value, flatIds);
        return new OmTerrainParseResult(flatIds, Optional.empty());
    }

    private static Optional<OmTerrainGrid> tryParseStringGrid(final JsonValue value) {
        final List<List<String>> rows = new ArrayList<>();
        for (JsonValue rowValue = value.child; rowValue != null; rowValue = rowValue.next) {
            if (!rowValue.isArray()) {
                return Optional.empty();
            }
            final List<String> row = new ArrayList<>();
            for (JsonValue cell = rowValue.child; cell != null; cell = cell.next) {
                if (!cell.isString()) {
                    return Optional.empty();
                }
                final String id = cell.asString();
                if (id == null || id.isEmpty()) {
                    return Optional.empty();
                }
                row.add(id);
            }
            if (row.isEmpty()) {
                return Optional.empty();
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OmTerrainGrid(rows));
    }

    private static void flattenOmTerrainArray(final JsonValue value, final List<String> flatIds) {
        for (JsonValue child = value.child; child != null; child = child.next) {
            if (child.isString()) {
                final String id = child.asString();
                if (id != null && !id.isEmpty()) {
                    flatIds.add(id);
                }
            } else if (child.isArray()) {
                flattenOmTerrainArray(child, flatIds);
            }
        }
    }

    private static final class OmTerrainParseResult {
        private final List<String> flatIds;
        private final Optional<OmTerrainGrid> grid;

        private OmTerrainParseResult(final List<String> flatIds, final Optional<OmTerrainGrid> grid) {
            this.flatIds = flatIds;
            this.grid = grid;
        }
    }
}
