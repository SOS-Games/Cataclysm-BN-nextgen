package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Options for {@link JsonMapgenRunner} (P2). */
public final class JsonMapgenRunOptions {

    private String defaultFillTer = "t_dirt";
    private long previewSeed = 0xC0DA1L;
    private String previewRegionId = "default";
    private int omtRotation = 0;
    private RegionContext regionContext;
    private MapgenCatalog mapgenCatalog;
    private List<String> neighborOmtIds = Collections.emptyList();
    private Map<String, String> neighborsByDirection = Collections.emptyMap();
    private Set<String> activeJoins = Collections.emptySet();
    private Map<String, String> rolledParameters = Collections.emptyMap();
    private LoadedGameData gameData;
    private final List<String> warnings = new ArrayList<>();
    private final List<SpawnMarker> spawnMarkers = new ArrayList<>();

    public JsonMapgenRunOptions() {}

    public String getDefaultFillTer() {
        return defaultFillTer;
    }

    public JsonMapgenRunOptions withDefaultFillTer(final String defaultFillTer) {
        this.defaultFillTer = defaultFillTer;
        return this;
    }

    public long getPreviewSeed() {
        return previewSeed;
    }

    public JsonMapgenRunOptions withPreviewSeed(final long previewSeed) {
        this.previewSeed = previewSeed;
        return this;
    }

    public Random createRng(final JsonMapgenDefinition definition) {
        final long salt = definition == null ? 0L : definition.displayName().hashCode();
        return new Random(previewSeed ^ salt);
    }

    public Random paletteRng() {
        return new Random(previewSeed ^ 0xC0DA10L);
    }

    public String getPreviewRegionId() {
        return previewRegionId;
    }

    public JsonMapgenRunOptions withPreviewRegionId(final String previewRegionId) {
        this.previewRegionId = previewRegionId == null || previewRegionId.isEmpty()
            ? "default"
            : previewRegionId;
        return this;
    }

    public int getOmtRotation() {
        return omtRotation;
    }

    public JsonMapgenRunOptions withOmtRotation(final int omtRotation) {
        this.omtRotation = Math.floorMod(omtRotation, 4);
        return this;
    }

    public JsonMapgenRunOptions deriveWithOmtRotation(final int omtRotation) {
        final JsonMapgenRunOptions derived = new JsonMapgenRunOptions();
        derived.defaultFillTer = defaultFillTer;
        derived.previewSeed = previewSeed;
        derived.previewRegionId = previewRegionId;
        derived.regionContext = regionContext;
        derived.mapgenCatalog = mapgenCatalog;
        derived.neighborOmtIds = neighborOmtIds;
        derived.neighborsByDirection = neighborsByDirection;
        derived.activeJoins = activeJoins;
        derived.rolledParameters = rolledParameters;
        derived.gameData = gameData;
        derived.omtRotation = Math.floorMod(omtRotation, 4);
        return derived;
    }

    public List<String> getNeighborOmtIds() {
        return neighborOmtIds;
    }

    public JsonMapgenRunOptions withNeighborOmtIds(final List<String> neighborOmtIds) {
        if (neighborOmtIds == null || neighborOmtIds.isEmpty()) {
            this.neighborOmtIds = Collections.emptyList();
        } else {
            this.neighborOmtIds = Collections.unmodifiableList(new ArrayList<>(neighborOmtIds));
        }
        return this;
    }

    public Map<String, String> getNeighborsByDirection() {
        return neighborsByDirection;
    }

    public JsonMapgenRunOptions withNeighborsByDirection(final Map<String, String> neighborsByDirection) {
        if (neighborsByDirection == null || neighborsByDirection.isEmpty()) {
            this.neighborsByDirection = Collections.emptyMap();
        } else {
            this.neighborsByDirection = Collections.unmodifiableMap(new HashMap<>(neighborsByDirection));
        }
        return this;
    }

    public Set<String> getActiveJoins() {
        return activeJoins;
    }

    public JsonMapgenRunOptions withActiveJoins(final Set<String> activeJoins) {
        if (activeJoins == null || activeJoins.isEmpty()) {
            this.activeJoins = Collections.emptySet();
        } else {
            this.activeJoins = Collections.unmodifiableSet(new HashSet<>(activeJoins));
        }
        return this;
    }

    public Map<String, String> getRolledParameters() {
        return rolledParameters;
    }

    public JsonMapgenRunOptions withRolledParameters(final Map<String, String> rolledParameters) {
        if (rolledParameters == null || rolledParameters.isEmpty()) {
            this.rolledParameters = Collections.emptyMap();
        } else {
            this.rolledParameters = Collections.unmodifiableMap(new HashMap<>(rolledParameters));
        }
        return this;
    }

    public RegionContext getRegionContext() {
        return regionContext;
    }

    public JsonMapgenRunOptions withRegionContext(final RegionContext regionContext) {
        this.regionContext = regionContext;
        return this;
    }

    public MapgenCatalog getMapgenCatalog() {
        return mapgenCatalog;
    }

    public JsonMapgenRunOptions withMapgenCatalog(final MapgenCatalog mapgenCatalog) {
        this.mapgenCatalog = mapgenCatalog;
        return this;
    }

    public LoadedGameData getGameData() {
        return gameData;
    }

    public JsonMapgenRunOptions withGameData(final LoadedGameData gameData) {
        this.gameData = gameData;
        return this;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public void addWarning(final String warning) {
        warnings.add(warning);
    }

    public List<SpawnMarker> getSpawnMarkers() {
        return Collections.unmodifiableList(spawnMarkers);
    }

    public void addSpawnMarkers(final List<SpawnMarker> markers) {
        if (markers == null || markers.isEmpty()) {
            return;
        }
        spawnMarkers.addAll(markers);
    }

    public List<SpawnMarker> drainSpawnMarkersSince(final int startIndex) {
        if (startIndex >= spawnMarkers.size()) {
            return List.of();
        }
        final List<SpawnMarker> drained = new ArrayList<>(spawnMarkers.subList(startIndex, spawnMarkers.size()));
        spawnMarkers.subList(startIndex, spawnMarkers.size()).clear();
        return drained;
    }

    public void clearSpawnMarkers() {
        spawnMarkers.clear();
    }
}
