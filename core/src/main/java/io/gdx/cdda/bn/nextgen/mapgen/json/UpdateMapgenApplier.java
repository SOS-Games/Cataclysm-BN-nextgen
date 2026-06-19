package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.Random;

/** Merges {@code update_mapgen_id} overlays onto an existing grid (P15). */
public final class UpdateMapgenApplier {

    private UpdateMapgenApplier() {}

    public static void mergeUpdate(
        final MapGrid base,
        final String updateMapgenId,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (base == null || updateMapgenId == null || updateMapgenId.isEmpty() || catalog == null || palettes == null) {
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final Random picker = rng == null ? runOptions.createRng(null) : rng;
        final JsonMapgenDefinition update = catalog.pickUpdateMapgen(updateMapgenId, picker).orElse(null);
        if (update == null) {
            runOptions.addWarning("unknown update_mapgen: " + updateMapgenId);
            return;
        }
        if (!update.isJsonPreviewSupported()) {
            runOptions.addWarning("unsupported update_mapgen: " + updateMapgenId);
            return;
        }
        JsonMapgenRunner.runOverlayOnto(base, update, catalog, palettes, runOptions, 0);
    }

    public static void mergeUpdate(
        final MapGrid base,
        final JsonMapgenDefinition updateDefinition,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options
    ) {
        if (base == null || updateDefinition == null || catalog == null || palettes == null) {
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        if (!updateDefinition.isJsonPreviewSupported()) {
            runOptions.addWarning("unsupported update mapgen: " + updateDefinition.displayName());
            return;
        }
        JsonMapgenRunner.runOverlayOnto(base, updateDefinition, catalog, palettes, runOptions, 0);
    }
}
