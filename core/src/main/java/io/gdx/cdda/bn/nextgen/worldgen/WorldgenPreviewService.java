package io.gdx.cdda.bn.nextgen.worldgen;

import io.gdx.cdda.bn.nextgen.gamedata.cache.JsonContentDiskCache;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoadResult;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateOptions;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateResult;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerator;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapNeighborGrid;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialLoadResult;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialLoader;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoadResult;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingIndex;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoadResult;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.submap.SubmapCache;
import io.gdx.cdda.bn.nextgen.worldgen.submap.SubmapGenerator;
import io.gdx.cdda.bn.nextgen.worldgen.submap.VisitResult;
import io.gdx.cdda.bn.nextgen.worldgen.visit.VolumeCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Facade for overmap terrain + visit-tile submap generation (W3). */
public final class WorldgenPreviewService {

    private static final int DEFAULT_CACHE_SIZE = 128;
    private static final int DEFAULT_VOLUME_CACHE_SIZE = 16;

    private final MapgenPreviewService mapgenPreviewService = new MapgenPreviewService();
    private SubmapCache submapCache = new SubmapCache(DEFAULT_CACHE_SIZE);
    private VolumeCache volumeCache = new VolumeCache(DEFAULT_VOLUME_CACHE_SIZE);

    private OvermapTerrainRegistry overmapTerrainRegistry = new OvermapTerrainRegistry();
    private OvermapConnectionRegistry overmapConnectionRegistry = new OvermapConnectionRegistry();
    private MutableSpecialRegistry mutableSpecialRegistry = new MutableSpecialRegistry();
    private RegionSettingsRegistry regionSettingsRegistry = RegionSettingsRegistry.empty();
    private PlacedBuildingIndex placementIndex = PlacedBuildingIndex.EMPTY;
    private List<String> overmapLoadWarnings = Collections.emptyList();
    private LoadedGameData gameData;
    private long worldSeed = 12345L;
    private String regionId = "default";
    private WorldgenWorldOptions worldOptions = WorldgenWorldOptions.bnDefaults();
    private boolean loaded;
    private OvermapNeighborGrid neighborGrid;
    private int neighborGridWidth;
    private int neighborGridHeight;

    public boolean hasDataRoots() {
        return !WorldgenScanOptions.defaults().getDataRoots().isEmpty();
    }

    public boolean isLoaded() {
        return loaded && mapgenPreviewService.isLoaded();
    }

    public MapgenPreviewService getMapgenPreviewService() {
        return mapgenPreviewService;
    }

    public OvermapTerrainRegistry getOvermapTerrainRegistry() {
        return overmapTerrainRegistry;
    }

    public OvermapConnectionRegistry getOvermapConnectionRegistry() {
        return overmapConnectionRegistry;
    }

    public MutableSpecialRegistry getMutableSpecialRegistry() {
        return mutableSpecialRegistry;
    }

    public RegionSettingsRegistry getRegionSettingsRegistry() {
        return regionSettingsRegistry;
    }

    public List<String> getOvermapLoadWarnings() {
        return overmapLoadWarnings;
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public void setWorldSeed(final long worldSeed) {
        this.worldSeed = worldSeed;
        submapCache.clear();
        volumeCache.clear();
        clearNeighborGrid();
    }

    public WorldgenWorldOptions getWorldOptions() {
        return worldOptions;
    }

    public void setWorldOptions(final WorldgenWorldOptions worldOptions) {
        this.worldOptions = worldOptions == null ? WorldgenWorldOptions.bnDefaults() : worldOptions;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(final String regionId) {
        final String next = regionId == null || regionId.isEmpty() ? "default" : regionId;
        if (next.equals(this.regionId)) {
            return;
        }
        this.regionId = next;
        submapCache.clear();
        volumeCache.clear();
        clearNeighborGrid();
    }

    public void setGameData(final LoadedGameData gameData) {
        this.gameData = gameData;
    }

    public void clearSubmapCache() {
        submapCache.clear();
        volumeCache.clear();
    }

    public int getSubmapCacheCapacity() {
        return submapCache.getMaxEntries();
    }

    public void setSubmapCacheCapacity(final int capacity) {
        submapCache = new SubmapCache(Math.max(1, capacity));
    }

    public int getVolumeCacheCapacity() {
        return volumeCache.getMaxEntries();
    }

    public void setVolumeCacheCapacity(final int capacity) {
        volumeCache = new VolumeCache(Math.max(1, capacity));
    }

    public PlacedBuildingIndex getPlacementIndex() {
        return placementIndex;
    }

    public synchronized void ensureLoaded(final WorldgenScanOptions options) throws IOException {
        if (loaded && mapgenPreviewService.isLoaded()) {
            return;
        }
        final WorldgenScanOptions scanOptions = options == null ? WorldgenScanOptions.defaults() : options;
        JsonContentDiskCache.withSession(
            "worldgen",
            scanOptions.getDataRoots(),
            scanOptions.getModIds(),
            () -> loadWorldgenCatalogs(scanOptions)
        );
    }

    private void loadWorldgenCatalogs(final WorldgenScanOptions scanOptions) throws IOException {
        final OvermapTerrainLoadResult oterResult = OvermapTerrainLoader.load(
            scanOptions.getOvermapTerrainScanOptions()
        );
        overmapTerrainRegistry = oterResult.getRegistry();
        overmapLoadWarnings = new ArrayList<>(oterResult.getWarnings());

        final OvermapConnectionLoadResult connectionResult = OvermapConnectionLoader.load(
            scanOptions.getOvermapConnectionScanOptions()
        );
        overmapConnectionRegistry = connectionResult.getRegistry();
        overmapLoadWarnings.addAll(connectionResult.getWarnings());

        final MutableSpecialLoadResult mutableResult = MutableSpecialLoader.load(
            scanOptions.getMutableSpecialScanOptions()
        );
        mutableSpecialRegistry = mutableResult.getRegistry();
        overmapLoadWarnings.addAll(mutableResult.getWarnings());

        final RegionSettingsLoadResult regionResult = RegionSettingsLoader.load(scanOptions.getMapgenScanOptions());
        regionSettingsRegistry = regionResult.getRegistry();
        overmapLoadWarnings.addAll(regionResult.getWarnings());

        mapgenPreviewService.ensureLoaded(scanOptions.getMapgenScanOptions());
        loaded = true;
    }

    public OvermapGrid createTestOvermap(final int width, final int height, final long seed) {
        worldSeed = seed;
        submapCache.clear();
        volumeCache.clear();
        if (!isLoaded()) {
            return OvermapGridFactory.empty(width, height, defaultFillId());
        }
        return generateOvermap(width, height).getGrid();
    }

    public OvermapGenerateResult generateOvermap(final int width, final int height) {
        return generateOvermap(
            OvermapGenerateOptions.forSize(width, height)
                .withSeed(worldSeed)
                .withWorldOptions(worldOptions)
                .withRegionId(regionId)
        );
    }

    public OvermapGenerateResult generateOvermap(final OvermapGenerateOptions options) {
        if (!isLoaded()) {
            return new OvermapGenerateResult(
                OvermapGridFactory.empty(
                    options == null ? 8 : options.getWidth(),
                    options == null ? 8 : options.getHeight(),
                    defaultFillId()
                ),
                Collections.singletonList("worldgen not loaded"),
                0,
                0
            );
        }
        final OvermapGenerateOptions resolved = resolveGenerateOptions(options);
        submapCache.clear();
        volumeCache.clear();
        final OvermapGenerateResult result = OvermapGenerator.generate(
            resolved,
            mapgenPreviewService.getCityBuildings(),
            overmapTerrainRegistry,
            overmapConnectionRegistry,
            mutableSpecialRegistry,
            regionSettingsRegistry
        );
        placementIndex = result.getPlacementIndex();
        return result;
    }

    /** Generate one overmap tile with west/north neighbor hydrology stitch (W16). */
    public OvermapGenerateResult generateOvermapAt(final int omX, final int omY, final int width, final int height) {
        if (!isLoaded()) {
            return new OvermapGenerateResult(
                OvermapGridFactory.empty(width, height, defaultFillId()),
                Collections.singletonList("worldgen not loaded"),
                0,
                0
            );
        }
        final OvermapGenerateOptions resolved = resolveGenerateOptions(
            OvermapGenerateOptions.forSize(width, height)
                .withWorldOptions(worldOptions)
                .withRegionId(regionId)
        );
        submapCache.clear();
        volumeCache.clear();
        final OvermapNeighborGrid grid = ensureNeighborGrid(resolved);
        final OvermapGenerateResult result = grid.getOrGenerate(omX, omY);
        placementIndex = result.getPlacementIndex();
        return result;
    }

    /** Generate a rectangular batch with cross-tile hydrology stitch and repolish (W16). */
    public List<OvermapGenerateResult> generateOvermapBatch(
        final int originOmX,
        final int originOmY,
        final int tilesX,
        final int tilesY,
        final int width,
        final int height
    ) {
        if (!isLoaded()) {
            return Collections.emptyList();
        }
        final OvermapGenerateOptions resolved = resolveGenerateOptions(
            OvermapGenerateOptions.forSize(width, height)
                .withWorldOptions(worldOptions)
                .withRegionId(regionId)
        );
        submapCache.clear();
        volumeCache.clear();
        final OvermapNeighborGrid grid = ensureNeighborGrid(resolved);
        return grid.generateBatch(originOmX, originOmY, tilesX, tilesY);
    }

    private OvermapNeighborGrid ensureNeighborGrid(final OvermapGenerateOptions baseOptions) {
        if (neighborGrid == null
            || neighborGridWidth != baseOptions.getWidth()
            || neighborGridHeight != baseOptions.getHeight()) {
            neighborGrid = new OvermapNeighborGrid(
                baseOptions,
                mapgenPreviewService.getCityBuildings(),
                overmapTerrainRegistry,
                overmapConnectionRegistry,
                mutableSpecialRegistry,
                regionSettingsRegistry
            );
            neighborGridWidth = baseOptions.getWidth();
            neighborGridHeight = baseOptions.getHeight();
        }
        return neighborGrid;
    }

    private void clearNeighborGrid() {
        neighborGrid = null;
        neighborGridWidth = 0;
        neighborGridHeight = 0;
    }

    private OvermapGenerateOptions resolveGenerateOptions(final OvermapGenerateOptions options) {
        final OvermapGenerateOptions base = options == null
            ? OvermapGenerateOptions.forSize(128, 128)
            : options;
        final String fieldId = resolveTerrainId(base.getFieldId(), "field");
        final String forestId = overmapTerrainRegistry.contains(base.getForestId())
            ? base.getForestId()
            : resolveTerrainId("forest", fieldId);
        final String riverCenterId = resolveTerrainId(base.getRiverCenterId(), "river_center", "test_river");
        final String riverBankId = resolveTerrainId(base.getRiverBankId(), "river", "test_river");
        final String connectionId = resolveConnectionId(base.getConnectionId());
        return base.withSeed(worldSeed)
            .withTerrainIds(fieldId, forestId)
            .withConnectivity(
                base.isRiversEnabled(),
                base.isRoadsEnabled(),
                connectionId,
                riverCenterId,
                riverBankId
            );
    }

    private String resolveConnectionId(final String preferred) {
        if (preferred != null && overmapConnectionRegistry.contains(preferred)) {
            return preferred;
        }
        if (overmapConnectionRegistry.contains("local_road")) {
            return "local_road";
        }
        if (overmapConnectionRegistry.contains("test_local_road")) {
            return "test_local_road";
        }
        return preferred == null || preferred.isEmpty() ? "local_road" : preferred;
    }

    private String resolveTerrainId(final String preferred, final String fallback) {
        return resolveTerrainId(preferred, fallback, null);
    }

    private String resolveTerrainId(final String preferred, final String fallback, final String secondFallback) {
        if (preferred != null && !preferred.isEmpty() && overmapTerrainRegistry.contains(preferred)) {
            return preferred;
        }
        if (fallback != null && !fallback.isEmpty() && overmapTerrainRegistry.contains(fallback)) {
            return fallback;
        }
        if (secondFallback != null && !secondFallback.isEmpty()
            && overmapTerrainRegistry.contains(secondFallback)) {
            return secondFallback;
        }
        if (overmapTerrainRegistry.contains("field")) {
            return "field";
        }
        if (overmapTerrainRegistry.contains("test_field")) {
            return "test_field";
        }
        if (overmapTerrainRegistry.contains("open_air")) {
            return "open_air";
        }
        return preferred == null || preferred.isEmpty() ? "field" : preferred;
    }

    public VisitResult visit(final OvermapGrid overmap, final int omtX, final int omtY) {
        return visit(overmap, omtX, omtY, 0);
    }

    public VisitResult visit(final OvermapGrid overmap, final int omtX, final int omtY, final int z) {
        if (!isLoaded()) {
            return new VisitResult(
                null,
                Collections.singletonList("worldgen not loaded"),
                false,
                ""
            );
        }
        return SubmapGenerator.visit(
            overmap,
            omtX,
            omtY,
            z,
            worldSeed,
            submapCache,
            volumeCache,
            placementIndex,
            mapgenPreviewService,
            overmapTerrainRegistry,
            gameData,
            mutableSpecialRegistry,
            overmapConnectionRegistry,
            resolvedRegion()
        );
    }

    private RegionSettingsDefinition resolvedRegion() {
        return regionSettingsRegistry.resolve(regionId).orElse(null);
    }

    private String defaultFillId() {
        if (overmapTerrainRegistry.contains("field")) {
            return "field";
        }
        if (overmapTerrainRegistry.contains("forest")) {
            return "forest";
        }
        return overmapTerrainRegistry.contains("open_air") ? "open_air" : "field";
    }
}
