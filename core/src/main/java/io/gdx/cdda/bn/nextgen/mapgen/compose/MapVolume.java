package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** One {@link MapGrid} per z-level for multi-floor building preview (P5). */
public final class MapVolume {

    private final String buildingId;
    private final List<Integer> zLevels;
    private final Map<Integer, MapGrid> gridsByZ;
    private final Map<Integer, List<OmtPieceRect>> pieceLayoutsByZ;
    private int activeZ;

    public MapVolume(
        final String buildingId,
        final List<Integer> zLevels,
        final Map<Integer, MapGrid> gridsByZ,
        final int activeZ
    ) {
        this(buildingId, zLevels, gridsByZ, Collections.emptyMap(), activeZ);
    }

    public MapVolume(
        final String buildingId,
        final List<Integer> zLevels,
        final Map<Integer, MapGrid> gridsByZ,
        final Map<Integer, List<OmtPieceRect>> pieceLayoutsByZ,
        final int activeZ
    ) {
        if (zLevels == null || zLevels.isEmpty()) {
            throw new IllegalArgumentException("zLevels must not be empty");
        }
        if (gridsByZ == null || gridsByZ.isEmpty()) {
            throw new IllegalArgumentException("gridsByZ must not be empty");
        }
        this.buildingId = buildingId == null ? "" : buildingId;
        this.zLevels = Collections.unmodifiableList(new ArrayList<>(zLevels));
        this.gridsByZ = Collections.unmodifiableMap(new LinkedHashMap<>(gridsByZ));
        this.pieceLayoutsByZ = pieceLayoutsByZ == null || pieceLayoutsByZ.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(pieceLayoutsByZ));
        if (!this.gridsByZ.containsKey(activeZ)) {
            throw new IllegalArgumentException("activeZ not present in gridsByZ: " + activeZ);
        }
        this.activeZ = activeZ;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public List<Integer> getZLevels() {
        return zLevels;
    }

    public int getActiveZ() {
        return activeZ;
    }

    public MapGrid getActiveGrid() {
        return gridsByZ.get(activeZ);
    }

    public MapGrid getGridAtZ(final int zLevel) {
        return gridsByZ.get(zLevel);
    }

    public void setActiveZ(final int zLevel) {
        if (!gridsByZ.containsKey(zLevel)) {
            throw new IllegalArgumentException("unknown z-level: " + zLevel);
        }
        activeZ = zLevel;
    }

    public Optional<Integer> nextZ() {
        int index = zLevels.indexOf(activeZ);
        if (index < 0 || index >= zLevels.size() - 1) {
            return Optional.empty();
        }
        return Optional.of(zLevels.get(index + 1));
    }

    public Optional<Integer> previousZ() {
        int index = zLevels.indexOf(activeZ);
        if (index <= 0) {
            return Optional.empty();
        }
        return Optional.of(zLevels.get(index - 1));
    }

    public int activeFloorIndex() {
        return zLevels.indexOf(activeZ);
    }

    public int floorCount() {
        return zLevels.size();
    }

    public List<OmtPieceRect> getPieceLayoutsAtZ(final int zLevel) {
        final List<OmtPieceRect> layouts = pieceLayoutsByZ.get(zLevel);
        return layouts == null ? Collections.emptyList() : layouts;
    }

    public List<OmtPieceRect> getActivePieceLayouts() {
        return getPieceLayoutsAtZ(activeZ);
    }
}
