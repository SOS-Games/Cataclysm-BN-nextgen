package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Result of visiting one OMT cell (W3). */
public final class VisitResult {

    private final MapGrid grid;
    private final List<String> warnings;
    private final List<SpawnMarker> spawnMarkers;
    private final boolean fromCache;
    private final String omtId;
    private final MapVolume volume;
    private final CityBuildingDefinition building;
    private final Map<Integer, List<SpawnMarker>> spawnMarkersByZ;
    private final int patchMinOmtX;
    private final int patchMinOmtY;
    private final int visitOmtX;
    private final int visitOmtY;

    public VisitResult(
        final MapGrid grid,
        final List<String> warnings,
        final boolean fromCache,
        final String omtId
    ) {
        this(grid, warnings, Collections.emptyList(), fromCache, omtId);
    }

    public VisitResult(
        final MapGrid grid,
        final List<String> warnings,
        final List<SpawnMarker> spawnMarkers,
        final boolean fromCache,
        final String omtId
    ) {
        this(grid, warnings, spawnMarkers, fromCache, omtId, null, null, Collections.emptyMap());
    }

    public VisitResult(
        final MapGrid grid,
        final List<String> warnings,
        final List<SpawnMarker> spawnMarkers,
        final boolean fromCache,
        final String omtId,
        final MapVolume volume,
        final CityBuildingDefinition building,
        final Map<Integer, List<SpawnMarker>> spawnMarkersByZ
    ) {
        this(
            grid,
            warnings,
            spawnMarkers,
            fromCache,
            omtId,
            volume,
            building,
            spawnMarkersByZ,
            -1,
            -1,
            -1,
            -1
        );
    }

    private VisitResult(
        final MapGrid grid,
        final List<String> warnings,
        final List<SpawnMarker> spawnMarkers,
        final boolean fromCache,
        final String omtId,
        final MapVolume volume,
        final CityBuildingDefinition building,
        final Map<Integer, List<SpawnMarker>> spawnMarkersByZ,
        final int patchMinOmtX,
        final int patchMinOmtY,
        final int visitOmtX,
        final int visitOmtY
    ) {
        this.grid = grid;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
        this.spawnMarkers = spawnMarkers == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(spawnMarkers));
        this.fromCache = fromCache;
        this.omtId = omtId == null ? "" : omtId;
        this.volume = volume;
        this.building = building;
        if (spawnMarkersByZ == null || spawnMarkersByZ.isEmpty()) {
            this.spawnMarkersByZ = Collections.emptyMap();
        } else {
            this.spawnMarkersByZ = Collections.unmodifiableMap(new LinkedHashMap<>(spawnMarkersByZ));
        }
        this.patchMinOmtX = patchMinOmtX;
        this.patchMinOmtY = patchMinOmtY;
        this.visitOmtX = visitOmtX;
        this.visitOmtY = visitOmtY;
    }

    public static VisitResult forBuilding(
        final MapGrid grid,
        final List<String> warnings,
        final MapVolume volume,
        final CityBuildingDefinition building,
        final Map<Integer, List<SpawnMarker>> spawnMarkersByZ,
        final boolean fromCache,
        final String omtId,
        final int visitOmtX,
        final int visitOmtY
    ) {
        final List<SpawnMarker> activeMarkers = volume == null
            ? Collections.emptyList()
            : markersAtZ(spawnMarkersByZ, volume.getActiveZ());
        return new VisitResult(
            grid,
            warnings,
            activeMarkers,
            fromCache,
            omtId,
            volume,
            building,
            spawnMarkersByZ,
            -1,
            -1,
            visitOmtX,
            visitOmtY
        );
    }

    public static VisitResult forPatch(
        final MapGrid grid,
        final List<String> warnings,
        final List<SpawnMarker> spawnMarkers,
        final boolean fromCache,
        final String omtId,
        final int patchMinOmtX,
        final int patchMinOmtY,
        final int visitOmtX,
        final int visitOmtY
    ) {
        return new VisitResult(
            grid,
            warnings,
            spawnMarkers,
            fromCache,
            omtId,
            null,
            null,
            Collections.emptyMap(),
            patchMinOmtX,
            patchMinOmtY,
            visitOmtX,
            visitOmtY
        );
    }

    public static VisitResult forBuilding(
        final MapGrid grid,
        final List<String> warnings,
        final MapVolume volume,
        final CityBuildingDefinition building,
        final Map<Integer, List<SpawnMarker>> spawnMarkersByZ,
        final boolean fromCache,
        final String omtId
    ) {
        return forBuilding(grid, warnings, volume, building, spawnMarkersByZ, fromCache, omtId, -1, -1);
    }

    private static List<SpawnMarker> markersAtZ(
        final Map<Integer, List<SpawnMarker>> spawnMarkersByZ,
        final int zLevel
    ) {
        if (spawnMarkersByZ == null || spawnMarkersByZ.isEmpty()) {
            return Collections.emptyList();
        }
        final List<SpawnMarker> markers = spawnMarkersByZ.get(zLevel);
        return markers == null ? Collections.emptyList() : markers;
    }

    public MapGrid getGrid() {
        return grid;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<SpawnMarker> getSpawnMarkers() {
        return spawnMarkers;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public String getOmtId() {
        return omtId;
    }

    public boolean hasGrid() {
        return grid != null;
    }

    public boolean isBuildingVisit() {
        return volume != null;
    }

    public Optional<MapVolume> getVolume() {
        return Optional.ofNullable(volume);
    }

    public Optional<CityBuildingDefinition> getBuilding() {
        return Optional.ofNullable(building);
    }

    public Map<Integer, List<SpawnMarker>> getSpawnMarkersByZ() {
        return spawnMarkersByZ;
    }

    public boolean isPatchVisit() {
        return patchMinOmtX >= 0 && patchMinOmtY >= 0;
    }

    public int getPatchMinOmtX() {
        return patchMinOmtX;
    }

    public int getPatchMinOmtY() {
        return patchMinOmtY;
    }

    public int getVisitOmtX() {
        return visitOmtX;
    }

    public int getVisitOmtY() {
        return visitOmtY;
    }
}
