package io.gdx.cdda.bn.nextgen.worldgen.connection;

import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed {@code type: overmap_connection} (W5, W11c directional). */
public final class OvermapConnectionDefinition {

    private final String id;
    private final String defaultTerrain;
    private final String bridgeTerrain;
    private final List<String> subtypeTerrains;

    public OvermapConnectionDefinition(
        final String id,
        final String defaultTerrain,
        final List<String> subtypeTerrains
    ) {
        this(id, defaultTerrain, null, subtypeTerrains);
    }

    public OvermapConnectionDefinition(
        final String id,
        final String defaultTerrain,
        final String bridgeTerrain,
        final List<String> subtypeTerrains
    ) {
        this.id = id;
        this.defaultTerrain = defaultTerrain == null || defaultTerrain.isEmpty() ? "road" : defaultTerrain;
        this.bridgeTerrain = bridgeTerrain;
        this.subtypeTerrains = subtypeTerrains == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(subtypeTerrains));
    }

    public String getId() {
        return id;
    }

    public String getDefaultTerrain() {
        return defaultTerrain;
    }

    public String getBridgeTerrain() {
        return bridgeTerrain;
    }

    public List<String> getSubtypeTerrains() {
        return subtypeTerrains;
    }

    /** v1 fallback: default terrain when direction is unknown. */
    public String resolveTerrainId() {
        return defaultTerrain;
    }

    public String pickTerrainForStep(
        final int fromX,
        final int fromY,
        final int toX,
        final int toY,
        final String existingOmtId,
        final OvermapGenerateOptions options
    ) {
        if (options != null && isRiverCenter(existingOmtId, options)) {
            if (bridgeTerrain != null && !bridgeTerrain.isEmpty()) {
                return bridgeTerrain;
            }
            return defaultTerrain;
        }
        final boolean eastWest = fromY == toY && fromX != toX;
        final boolean northSouth = fromX == toX && fromY != toY;
        for (final String terrainId : subtypeTerrains) {
            if (terrainId == null || terrainId.isEmpty()) {
                continue;
            }
            if (eastWest && terrainId.endsWith("_ew")) {
                return terrainId;
            }
            if (northSouth && terrainId.endsWith("_ns")) {
                return terrainId;
            }
        }
        return defaultTerrain;
    }

    private static boolean isRiverCenter(final String omtId, final OvermapGenerateOptions options) {
        if (omtId == null || omtId.isEmpty() || options == null) {
            return false;
        }
        return omtId.equals(options.getRiverCenterId()) || omtId.equals("river_center") || omtId.equals("test_river");
    }
}
