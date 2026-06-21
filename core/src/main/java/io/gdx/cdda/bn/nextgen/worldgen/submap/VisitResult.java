package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of visiting one OMT cell (W3). */
public final class VisitResult {

    private final MapGrid grid;
    private final List<String> warnings;
    private final List<SpawnMarker> spawnMarkers;
    private final boolean fromCache;
    private final String omtId;

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
        this.grid = grid;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
        this.spawnMarkers = spawnMarkers == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(spawnMarkers));
        this.fromCache = fromCache;
        this.omtId = omtId == null ? "" : omtId;
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
}
