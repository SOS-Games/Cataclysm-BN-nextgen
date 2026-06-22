package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Matches join ids between mutable special pieces (W6, W11a rotation). */
public final class JoinMatcher {

    private JoinMatcher() {}

    public static boolean joinsMatch(
        final String leftJoinId,
        final String rightJoinId,
        final Map<String, String> joinOpposites
    ) {
        if (leftJoinId == null || rightJoinId == null || leftJoinId.isEmpty() || rightJoinId.isEmpty()) {
            return false;
        }
        if (leftJoinId.equals(rightJoinId)) {
            return true;
        }
        final String leftOpposite = joinOpposites.get(leftJoinId);
        if (rightJoinId.equals(leftOpposite)) {
            return true;
        }
        final String rightOpposite = joinOpposites.get(rightJoinId);
        return leftJoinId.equals(rightOpposite);
    }

    public static Optional<int[]> findAttachment(
        final List<PlacedMutablePiece> placed,
        final MutableOvermapNode candidate,
        final MutableSpecialDefinition definition
    ) {
        if (placed == null || placed.isEmpty() || candidate == null || definition == null) {
            return Optional.empty();
        }
        final Map<String, String> joinOpposites = definition.getJoinOpposites();
        for (int rotation = 0; rotation < 4; rotation++) {
            for (final PlacedMutablePiece placedPiece : placed) {
                final MutableOvermapNode placedNode = definition.getNode(placedPiece.getPieceId());
                if (placedNode == null) {
                    continue;
                }
                for (final CardinalDirection direction : CardinalDirection.values()) {
                    final String outgoingJoin = edgeJoinAtWorldDirection(
                        placedNode,
                        direction,
                        placedPiece.getRotation()
                    );
                    if (outgoingJoin == null) {
                        continue;
                    }
                    final CardinalDirection incomingDirection = direction.opposite();
                    final String incomingJoin = edgeJoinAtWorldDirection(
                        candidate,
                        incomingDirection,
                        rotation
                    );
                    if (!joinsMatch(outgoingJoin, incomingJoin, joinOpposites)) {
                        continue;
                    }
                    final int targetX = placedPiece.getOffsetX() + direction.getDx();
                    final int targetY = placedPiece.getOffsetY() + direction.getDy();
                    if (isOccupied(placed, targetX, targetY)) {
                        continue;
                    }
                    return Optional.of(new int[] { targetX, targetY, rotation });
                }
            }
        }
        return Optional.empty();
    }

    private static String edgeJoinAtWorldDirection(
        final MutableOvermapNode node,
        final CardinalDirection worldDirection,
        final int rotation
    ) {
        final CardinalDirection localDirection = worldDirection.rotateClockwise(4 - Math.floorMod(rotation, 4));
        return node.getEdgeJoin(localDirection);
    }

    private static boolean isOccupied(
        final List<PlacedMutablePiece> placed,
        final int x,
        final int y
    ) {
        for (final PlacedMutablePiece piece : placed) {
            if (piece.getOffsetX() == x && piece.getOffsetY() == y) {
                return true;
            }
        }
        return false;
    }
}
