package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds {@link OvermapGrid} instances for tests and editor smoke (W2). */
public final class OvermapGridFactory {

    private static final int SMOKE_GRID_SIZE = 8;
    private static final int MAX_SMOKE_SAMPLES = 8;

    private OvermapGridFactory() {}

    public static OvermapGrid empty(final int width, final int height, final String fillId) {
        return new OvermapGrid(width, height, fillId);
    }

    /** Editor smoke layout: land background plus a few visitable json mapgen OMT ids. */
    public static OvermapGrid smokeFromCatalog(
        final OvermapTerrainRegistry registry,
        final MapgenCatalog catalog
    ) {
        final OvermapGrid grid = empty(SMOKE_GRID_SIZE, SMOKE_GRID_SIZE, defaultFill(registry));
        if (catalog == null) {
            placeRegistryLandmarks(registry, grid);
            return grid;
        }

        int x = 1;
        int y = 1;
        int placed = 0;
        final Set<String> placedIds = new HashSet<>();
        for (final JsonMapgenDefinition definition : catalog.runnableOnly()) {
            if (!definition.isStandalonePickerEntry()) {
                continue;
            }
            final List<String> omTerrains = definition.getOmTerrain();
            if (omTerrains.isEmpty()) {
                continue;
            }
            final String omtId = omTerrains.get(0);
            if (omtId == null || omtId.isEmpty() || placedIds.contains(omtId)) {
                continue;
            }
            if (y >= SMOKE_GRID_SIZE - 1) {
                break;
            }
            if (x >= SMOKE_GRID_SIZE - 1) {
                x = 1;
                y += 2;
            }
            if (y >= SMOKE_GRID_SIZE - 1) {
                break;
            }
            grid.setOmtId(x, y, omtId);
            placedIds.add(omtId);
            x += 2;
            placed++;
            if (placed >= MAX_SMOKE_SAMPLES) {
                break;
            }
        }

        if (placed == 0) {
            placeRegistryLandmarks(registry, grid);
        }
        return grid;
    }

    public static boolean isVisitableOmt(
        final String omtId,
        final MapgenCatalog catalog
    ) {
        if (omtId == null || omtId.isEmpty() || catalog == null) {
            return false;
        }
        return OvermapTerrainResolver.resolveMapgen(catalog, omtId).isPresent();
    }

    private static String defaultFill(final OvermapTerrainRegistry registry) {
        if (registry != null && registry.contains("field")) {
            return "field";
        }
        if (registry != null && registry.contains("forest")) {
            return "forest";
        }
        if (registry != null && registry.contains("open_air")) {
            return "open_air";
        }
        return "field";
    }

    private static void placeRegistryLandmarks(
        final OvermapTerrainRegistry registry,
        final OvermapGrid grid
    ) {
        if (registry == null) {
            return;
        }
        if (registry.contains("forest")) {
            grid.setOmtId(2, 2, "forest");
            grid.setOmtId(3, 2, "forest");
        }
        if (registry.contains("field")) {
            grid.setOmtId(2, 4, "field");
            grid.setOmtId(3, 4, "field");
        }
    }

    public static OvermapGrid fromJsonFile(final Path file) throws IOException {
        final String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        return fromJson(text);
    }

    public static OvermapGrid fromJson(final String jsonText) {
        final JsonValue root = new JsonReader().parse(jsonText);
        if (!root.isObject()) {
            throw new IllegalArgumentException("overmap fixture root must be an object");
        }
        final int width = root.getInt("width", 0);
        final int height = root.getInt("height", 0);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height are required");
        }
        final String fill = root.getString("fill", "open_air");
        final String[] cells = new String[width * height];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = fill;
        }

        final JsonValue rows = root.get("rows");
        if (rows != null && rows.isArray()) {
            int rowIndex = 0;
            for (JsonValue row = rows.child; row != null && rowIndex < height; row = row.next, rowIndex++) {
                if (!row.isArray()) {
                    continue;
                }
                int colIndex = 0;
                for (JsonValue cell = row.child; cell != null && colIndex < width; cell = cell.next, colIndex++) {
                    final String id = cell.asString();
                    if (id != null && !id.isEmpty()) {
                        cells[rowIndex * width + colIndex] = id;
                    }
                }
            }
        }

        final JsonValue patches = root.get("patches");
        if (patches != null && patches.isArray()) {
            for (JsonValue patch = patches.child; patch != null; patch = patch.next) {
                if (!patch.isObject()) {
                    continue;
                }
                final int x = patch.getInt("x", -1);
                final int y = patch.getInt("y", -1);
                final String id = patch.getString("id", null);
                if (x < 0 || y < 0 || x >= width || y >= height || id == null || id.isEmpty()) {
                    continue;
                }
                cells[y * width + x] = id;
            }
        }

        return OvermapGrid.fromCells(width, height, cells);
    }
}
