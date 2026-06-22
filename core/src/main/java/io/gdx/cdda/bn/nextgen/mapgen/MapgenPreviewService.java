package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingLoader;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.building.MapgenFileBundleInferrer;
import io.gdx.cdda.bn.nextgen.mapgen.compose.BuildingPlacementContext;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolumeBuilder;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolumeBuilder.MapVolumeBuildResult;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalogResult;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialLoadResult;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialLoader;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialScanOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads mapgen catalogs and generates preview grids (P3). */
public final class MapgenPreviewService {

    private PaletteRegistry palettes;
    private MapgenCatalog catalog;
    private RegionContext regionContext = RegionContext.empty();
    private CityBuildingRegistry cityBuildings = CityBuildingRegistry.empty();
    private MutableSpecialRegistry mutableSpecials = MutableSpecialRegistry.empty();
    private MapgenPickerIndex pickerIndex = MapgenPickerIndex.build(null, CityBuildingRegistry.empty());
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

    public CityBuildingRegistry getCityBuildings() {
        if (!loaded) {
            throw new IllegalStateException("call ensureLoaded before getCityBuildings");
        }
        return cityBuildings;
    }

    public MutableSpecialRegistry getMutableSpecials() {
        if (!loaded) {
            throw new IllegalStateException("call ensureLoaded before getMutableSpecials");
        }
        return mutableSpecials;
    }

    public MapgenPickerIndex getPickerIndex() {
        if (!loaded) {
            throw new IllegalStateException("call ensureLoaded before getPickerIndex");
        }
        return pickerIndex;
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

        cityBuildings = MapgenFileBundleInferrer.augment(
            CityBuildingLoader.load(scanOptions),
            catalog
        );
        warnings.addAll(cityBuildings.getWarnings());

        final MutableSpecialLoadResult mutableResult = MutableSpecialLoader.load(
            new MutableSpecialScanOptions(scanOptions.getDataRoots(), scanOptions.getModIds())
        );
        mutableSpecials = mutableResult.getRegistry();
        warnings.addAll(mutableResult.getWarnings());

        pickerIndex = MapgenPickerIndex.build(catalog, cityBuildings, mutableSpecials);

        regionContext = RegionContext.load(scanOptions, warnings);

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

        final JsonMapgenRunOptions options = withLoadedContext(
            runOptions == null ? new JsonMapgenRunOptions() : runOptions
        );
        if (gameData != null) {
            options.withGameData(gameData);
        }
        final MapGrid grid = JsonMapgenRunner.run(definition, catalog, palettes, options);
        return new MapgenPreviewResult(grid, options.getSpawnMarkers(), options.getWarnings());
    }

    public MapgenBuildingResult generateBuilding(
        final CityBuildingDefinition building,
        final JsonMapgenRunOptions runOptions
    ) {
        return generateBuilding(building, null, runOptions);
    }

    public MapgenBuildingResult generateBuilding(
        final CityBuildingDefinition building,
        final LoadedGameData gameData,
        final JsonMapgenRunOptions runOptions
    ) {
        return generateBuilding(building, gameData, runOptions, null);
    }

    public MapgenBuildingResult generateBuilding(
        final CityBuildingDefinition building,
        final LoadedGameData gameData,
        final JsonMapgenRunOptions runOptions,
        final BuildingPlacementContext placementContext
    ) {
        if (!loaded || palettes == null || catalog == null) {
            throw new IllegalStateException("call ensureLoaded before generateBuilding");
        }
        if (building == null) {
            throw new IllegalArgumentException("building is required");
        }
        final JsonMapgenRunOptions options = withLoadedContext(
            runOptions == null ? new JsonMapgenRunOptions() : runOptions
        );
        if (gameData != null) {
            options.withGameData(gameData);
        }
        final MapVolumeBuildResult built = MapVolumeBuilder.build(
            building,
            catalog,
            palettes,
            options,
            placementContext
        );
        return new MapgenBuildingResult(
            built.getVolume(),
            built.getWarnings(),
            built.getSpawnMarkersByZ()
        );
    }

    private JsonMapgenRunOptions withLoadedContext(final JsonMapgenRunOptions options) {
        final JsonMapgenRunOptions resolved = options.getRegionContext() != null
            ? options
            : options.withRegionContext(regionContext);
        if (resolved.getMapgenCatalog() != null) {
            return resolved;
        }
        return resolved.withMapgenCatalog(catalog);
    }

    public static final class MapgenBuildingResult {
        private final MapVolume volume;
        private final List<String> runWarnings;
        private final Map<Integer, List<SpawnMarker>> spawnMarkersByZ;

        public MapgenBuildingResult(
            final MapVolume volume,
            final List<String> runWarnings,
            final Map<Integer, List<SpawnMarker>> spawnMarkersByZ
        ) {
            this.volume = volume;
            this.runWarnings = Collections.unmodifiableList(new ArrayList<>(runWarnings));
            this.spawnMarkersByZ = spawnMarkersByZ == null || spawnMarkersByZ.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(spawnMarkersByZ));
        }

        public MapVolume getVolume() {
            return volume;
        }

        public List<String> getRunWarnings() {
            return runWarnings;
        }

        public Map<Integer, List<SpawnMarker>> getSpawnMarkersByZ() {
            return spawnMarkersByZ;
        }
    }

    public static final class MapgenPreviewResult {
        private final MapGrid grid;
        private final List<SpawnMarker> spawnMarkers;
        private final List<String> runWarnings;

        public MapgenPreviewResult(
            final MapGrid grid,
            final List<SpawnMarker> spawnMarkers,
            final List<String> runWarnings
        ) {
            this.grid = grid;
            this.spawnMarkers = spawnMarkers == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(spawnMarkers));
            this.runWarnings = Collections.unmodifiableList(new ArrayList<>(runWarnings));
        }

        public MapGrid getGrid() {
            return grid;
        }

        public List<SpawnMarker> getSpawnMarkers() {
            return spawnMarkers;
        }

        public List<String> getRunWarnings() {
            return runWarnings;
        }
    }
}
