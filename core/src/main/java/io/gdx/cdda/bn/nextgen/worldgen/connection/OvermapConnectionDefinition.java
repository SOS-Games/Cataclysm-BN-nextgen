package io.gdx.cdda.bn.nextgen.worldgen.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed {@code type: overmap_connection} (W5). */
public final class OvermapConnectionDefinition {

    private final String id;
    private final String defaultTerrain;
    private final List<String> subtypeTerrains;

    public OvermapConnectionDefinition(
        final String id,
        final String defaultTerrain,
        final List<String> subtypeTerrains
    ) {
        this.id = id;
        this.defaultTerrain = defaultTerrain == null || defaultTerrain.isEmpty() ? "road" : defaultTerrain;
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

    public List<String> getSubtypeTerrains() {
        return subtypeTerrains;
    }

    /** v1: always use {@link #defaultTerrain}; subtype weights deferred. */
    public String resolveTerrainId() {
        return defaultTerrain;
    }
}
