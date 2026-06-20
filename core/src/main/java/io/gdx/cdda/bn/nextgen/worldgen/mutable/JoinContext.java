package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Neighbor OMT + join context for nested mapgen (W6 stub for W3). */
public final class JoinContext {

    private final Map<CardinalDirection, String> neighborOmtIds;
    private final Set<String> activeJoins;

    public JoinContext(
        final Map<CardinalDirection, String> neighborOmtIds,
        final Set<String> activeJoins
    ) {
        this.neighborOmtIds = neighborOmtIds == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new EnumMap<>(neighborOmtIds));
        this.activeJoins = activeJoins == null
            ? Collections.emptySet()
            : Collections.unmodifiableSet(new HashSet<>(activeJoins));
    }

    public static JoinContext fromOvermap(final OvermapGrid overmap, final int omtX, final int omtY) {
        if (overmap == null) {
            return new JoinContext(Collections.emptyMap(), Collections.emptySet());
        }
        final EnumMap<CardinalDirection, String> neighbors = new EnumMap<>(CardinalDirection.class);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.NORTH, neighbors);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.EAST, neighbors);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.SOUTH, neighbors);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.WEST, neighbors);
        return new JoinContext(neighbors, Collections.emptySet());
    }

    private static void putNeighbor(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final CardinalDirection direction,
        final Map<CardinalDirection, String> neighbors
    ) {
        final int x = omtX + direction.getDx();
        final int y = omtY + direction.getDy();
        if (x < 0 || y < 0 || x >= overmap.width() || y >= overmap.height()) {
            return;
        }
        neighbors.put(direction, overmap.getOmtId(x, y));
    }

    public Map<CardinalDirection, String> getNeighborOmtIds() {
        return neighborOmtIds;
    }

    public Set<String> getActiveJoins() {
        return activeJoins;
    }
}
