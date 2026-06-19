package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.Optional;

/** Resolves and paints {@code predecessor_mapgen} underlays (P12). */
public final class PredecessorMapgenRunner {

    static final int MAX_DEPTH = 4;

    private PredecessorMapgenRunner() {}

    public static boolean hasPredecessorMapgen(final JsonValue object) {
        if (object == null || !object.isObject()) {
            return false;
        }
        final String predecessorId = object.getString("predecessor_mapgen", null);
        return predecessorId != null && !predecessorId.isEmpty();
    }

    public static Optional<JsonMapgenDefinition> resolve(
        final MapgenCatalog catalog,
        final String predecessorOmTerrain
    ) {
        if (catalog == null || predecessorOmTerrain == null || predecessorOmTerrain.isEmpty()) {
            return Optional.empty();
        }
        return catalog.findFirstRunnableByOmTerrain(predecessorOmTerrain);
    }

    public static void paintPredecessor(
        final MapGrid canvas,
        final JsonValue object,
        final JsonMapgenDefinition definition,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final int depth
    ) {
        if (canvas == null || object == null || catalog == null || palettes == null) {
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final String predecessorId = object.getString("predecessor_mapgen", null);
        if (predecessorId == null || predecessorId.isEmpty()) {
            return;
        }
        if (depth >= MAX_DEPTH) {
            runOptions.addWarning(
                "predecessor_mapgen depth limit (" + MAX_DEPTH + ") exceeded at "
                    + (definition == null ? predecessorId : definition.displayName())
            );
            return;
        }

        final Optional<JsonMapgenDefinition> predecessor = resolve(catalog, predecessorId);
        if (predecessor.isEmpty()) {
            runOptions.addWarning("unknown predecessor_mapgen: " + predecessorId);
            return;
        }

        final MapGrid predecessorGrid = JsonMapgenRunner.runInternal(
            predecessor.get(),
            catalog,
            palettes,
            runOptions,
            depth + 1
        );
        if (predecessorGrid.width() != canvas.width() || predecessorGrid.height() != canvas.height()) {
            runOptions.addWarning(
                "predecessor_mapgen " + predecessorId + " size "
                    + predecessorGrid.width() + "x" + predecessorGrid.height()
                    + " does not match canvas " + canvas.width() + "x" + canvas.height()
            );
        }
        canvas.blitFrom(predecessorGrid, 0, 0, null);
    }

    static int canvasSizeWithPredecessor(final int contentSize) {
        return Math.max(contentSize, OmtStitchComposer.DEFAULT_OMT_SIZE);
    }
}
