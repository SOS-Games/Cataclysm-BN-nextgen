package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionResolver;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacementSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Neighbor OMT + join + connection context for nested mapgen (W6, W11d, W13c). */
public final class JoinContext {

    private final Map<CardinalDirection, String> neighborOmtIds;
    private final Set<String> activeJoins;
    private final Map<String, String> connectionsByDirection;

    public JoinContext(
        final Map<CardinalDirection, String> neighborOmtIds,
        final Set<String> activeJoins
    ) {
        this(neighborOmtIds, activeJoins, Collections.emptyMap());
    }

    public JoinContext(
        final Map<CardinalDirection, String> neighborOmtIds,
        final Set<String> activeJoins,
        final Map<String, String> connectionsByDirection
    ) {
        this.neighborOmtIds = neighborOmtIds == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new EnumMap<>(neighborOmtIds));
        this.activeJoins = activeJoins == null
            ? Collections.emptySet()
            : Collections.unmodifiableSet(new HashSet<>(activeJoins));
        this.connectionsByDirection = connectionsByDirection == null || connectionsByDirection.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(connectionsByDirection));
    }

    public static JoinContext fromOvermap(final OvermapGrid overmap, final int omtX, final int omtY) {
        return fromOvermap(overmap, omtX, omtY, null);
    }

    public static JoinContext fromOvermap(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        if (overmap == null) {
            return new JoinContext(Collections.emptyMap(), Collections.emptySet());
        }
        final EnumMap<CardinalDirection, String> neighbors = new EnumMap<>(CardinalDirection.class);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.NORTH, neighbors);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.EAST, neighbors);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.SOUTH, neighbors);
        putNeighbor(overmap, omtX, omtY, CardinalDirection.WEST, neighbors);
        final Map<String, String> connections = OvermapConnectionResolver.connectionsByDirection(
            overmap,
            omtX,
            omtY,
            connectionRegistry
        );
        return new JoinContext(neighbors, Collections.emptySet(), connections);
    }

    public static JoinContext fromPlacement(
        final OvermapGrid overmap,
        final PlacedBuildingRecord record,
        final int omtX,
        final int omtY,
        final MutableSpecialDefinition definition
    ) {
        return fromPlacement(overmap, record, omtX, omtY, definition, null);
    }

    public static JoinContext fromPlacement(
        final OvermapGrid overmap,
        final PlacedBuildingRecord record,
        final int omtX,
        final int omtY,
        final MutableSpecialDefinition definition,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        final JoinContext base = fromOvermap(overmap, omtX, omtY, connectionRegistry);
        if (record == null
            || record.getSource() != PlacementSource.MUTABLE
            || !record.getMutableLayout().isPresent()
            || definition == null) {
            return base;
        }
        final AssembledSpecialLayout layout = record.getMutableLayout().get();
        final int layoutX = omtX - record.getAnchorX();
        final int layoutY = omtY - record.getAnchorY();
        final Set<String> activeJoins = activeJoinsAt(layout, layoutX, layoutY, definition);
        return new JoinContext(base.getNeighborOmtIds(), activeJoins, base.getConnectionsByDirection());
    }

    private static Set<String> activeJoinsAt(
        final AssembledSpecialLayout layout,
        final int layoutX,
        final int layoutY,
        final MutableSpecialDefinition definition
    ) {
        final PlacedMutablePiece current = findPieceAt(layout.getPieces(), layoutX, layoutY);
        if (current == null) {
            return Collections.emptySet();
        }
        final MutableOvermapNode node = definition.getNode(current.getPieceId());
        if (node == null) {
            return Collections.emptySet();
        }
        final Set<String> joins = new HashSet<>();
        for (final CardinalDirection direction : CardinalDirection.values()) {
            final String joinId = edgeJoinAtWorldDirection(node, direction, current.getRotation());
            if (joinId == null || joinId.isEmpty()) {
                continue;
            }
            final int neighborX = layoutX + direction.getDx();
            final int neighborY = layoutY + direction.getDy();
            if (findPieceAt(layout.getPieces(), neighborX, neighborY) != null) {
                joins.add(joinId);
            }
        }
        return joins;
    }

    private static PlacedMutablePiece findPieceAt(
        final List<PlacedMutablePiece> pieces,
        final int x,
        final int y
    ) {
        for (final PlacedMutablePiece piece : pieces) {
            if (piece.getOffsetX() == x && piece.getOffsetY() == y) {
                return piece;
            }
        }
        return null;
    }

    private static String edgeJoinAtWorldDirection(
        final MutableOvermapNode node,
        final CardinalDirection worldDirection,
        final int rotation
    ) {
        final CardinalDirection localDirection = worldDirection.rotateClockwise(4 - Math.floorMod(rotation, 4));
        return node.getEdgeJoin(localDirection);
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

    public Map<String, String> getNeighborsByDirection() {
        final Map<String, String> neighbors = new java.util.LinkedHashMap<>();
        for (final Map.Entry<CardinalDirection, String> entry : neighborOmtIds.entrySet()) {
            neighbors.put(entry.getKey().name().toLowerCase(java.util.Locale.ROOT), entry.getValue());
        }
        return neighbors;
    }

    public Set<String> getActiveJoins() {
        return activeJoins;
    }

    public Map<String, String> getConnectionsByDirection() {
        return connectionsByDirection;
    }
}
