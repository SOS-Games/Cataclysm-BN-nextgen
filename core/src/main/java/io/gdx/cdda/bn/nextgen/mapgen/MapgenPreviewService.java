package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalogResult;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads mapgen catalogs and generates preview grids (P3). */
public final class MapgenPreviewService {

    private PaletteRegistry palettes;
    private MapgenCatalog catalog;
    private List<String> loadWarnings = Collections.emptyList();
    private boolean loaded;

    public boolean hasDataRoots() {
        return !MapgenScanOptions.defaults().getDataRoots().isEmpty();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public MapgenCatalog getCatalog() {
        if (!loaded || catalog == null) {
            throw new IllegalStateException("call ensureLoaded before getCatalog");
        }
        return catalog;
    }

    public List<String> getLoadWarnings() {
        return loadWarnings;
    }

    public synchronized void ensureLoaded(final MapgenScanOptions options) throws IOException {
        if (loaded) {
            return;
        }
        final MapgenScanOptions scanOptions = options == null ? MapgenScanOptions.defaults() : options;
        final List<String> warnings = new ArrayList<>();

        final MapgenLoadResult paletteResult = PaletteLoader.load(scanOptions);
        palettes = paletteResult.getPalettes();
        warnings.addAll(paletteResult.getWarnings());

        final MapgenCatalogResult catalogResult = JsonMapgenLoader.load(scanOptions);
        catalog = catalogResult.getCatalog();
        warnings.addAll(catalogResult.getWarnings());

        loadWarnings = Collections.unmodifiableList(warnings);
        loaded = true;
    }

    public MapgenPreviewResult generate(
        final JsonMapgenDefinition definition,
        final LoadedGameData gameData,
        final JsonMapgenRunOptions runOptions
    ) {
        if (!loaded || palettes == null) {
            throw new IllegalStateException("call ensureLoaded before generate");
        }
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        if (!definition.isJsonPreviewSupported()) {
            throw new IllegalArgumentException("mapgen is not runnable: " + definition.displayName());
        }

        final JsonMapgenRunOptions options = runOptions == null ? new JsonMapgenRunOptions() : runOptions;
        final MapGrid grid = JsonMapgenRunner.run(definition, palettes, options);
        return new MapgenPreviewResult(grid, options.getWarnings());
    }

    public static final class MapgenPreviewResult {
        private final MapGrid grid;
        private final List<String> runWarnings;

        public MapgenPreviewResult(final MapGrid grid, final List<String> runWarnings) {
            this.grid = grid;
            this.runWarnings = Collections.unmodifiableList(new ArrayList<>(runWarnings));
        }

        public MapGrid getGrid() {
            return grid;
        }

        public List<String> getRunWarnings() {
            return runWarnings;
        }
    }
}
