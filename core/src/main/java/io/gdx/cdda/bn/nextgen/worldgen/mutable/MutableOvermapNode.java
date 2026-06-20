package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** One named node in a mutable {@code overmap_special} (W6). */
public final class MutableOvermapNode {

    private final String pieceId;
    private final String overmapTerrainId;
    private final Map<CardinalDirection, String> edgeJoins;

    public MutableOvermapNode(
        final String pieceId,
        final String overmapTerrainId,
        final Map<CardinalDirection, String> edgeJoins
    ) {
        this.pieceId = pieceId;
        this.overmapTerrainId = overmapTerrainId;
        this.edgeJoins = edgeJoins == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new EnumMap<>(edgeJoins));
    }

    public String getPieceId() {
        return pieceId;
    }

    public String getOvermapTerrainId() {
        return overmapTerrainId;
    }

    public String getEdgeJoin(final CardinalDirection direction) {
        return edgeJoins.get(direction);
    }

    public Map<CardinalDirection, String> getEdgeJoins() {
        return edgeJoins;
    }
}
