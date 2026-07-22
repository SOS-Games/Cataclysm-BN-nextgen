package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapSpecialConnection;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BN {@code place_special} connection pass: carve {@code local_road} (etc.) from a special's
 * connection points to nearby roads, or paint a one-cell stub.
 */
public final class SpecialRoadStitcher {

    private static final int SEARCH_RADIUS = 24;

    private SpecialRoadStitcher() {}

    public static int stitch(
        final OvermapGrid grid,
        final CityBuildingDefinition building,
        final int anchorX,
        final int anchorY,
        final int rotationQuarterTurns,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (grid == null || building == null || building.getConnections().isEmpty() || options == null) {
            return 0;
        }
        int painted = 0;
        for (final OvermapSpecialConnection conn : building.getConnections()) {
            if (conn.getOffsetZ() != 0) {
                continue;
            }
            painted += stitchOne(
                grid, building, anchorX, anchorY, rotationQuarterTurns, conn, connections, options, registry
            );
        }
        return painted;
    }

    private static int stitchOne(
        final OvermapGrid grid,
        final CityBuildingDefinition building,
        final int anchorX,
        final int anchorY,
        final int rotationQuarterTurns,
        final OvermapSpecialConnection conn,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final int[] world = rotateOffset(conn.getOffsetX(), conn.getOffsetY(), rotationQuarterTurns);
        final int x = anchorX + world[0];
        final int y = anchorY + world[1];
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return 0;
        }

        if (conn.isExisting()) {
            return RoadConnectionPolisher.isRoadFamily(grid.getOmtId(x, y), connections) ? 0 : 0;
        }
        if (RoadConnectionPolisher.isRoadFamily(grid.getOmtId(x, y), connections)) {
            return 0;
        }

        final OvermapConnectionDefinition connection = resolveConnection(connections, conn.getConnectionId());
        final String roadId = connection != null
            ? OrthogonalPathCarver.resolveTerrainId(connection.resolveTerrainId(), "test_road", registry)
            : OrthogonalPathCarver.resolveTerrainId("road", "test_road", registry);
        final Set<String> overwritable = new HashSet<>(
            OrthogonalPathCarver.terrainOverwritableIds(options, registry, roadId)
        );

        final int[] nearest = findNearestRoad(grid, x, y, connections, building, anchorX, anchorY);
        if (nearest != null) {
            final List<int[]> path = OrthogonalPathCarver.buildPath(x, y, nearest[0], nearest[1], null);
            return OrthogonalPathCarver.paintDirectionalPath(
                grid,
                path,
                (fromX, fromY, toX, toY, existing) -> {
                    if (UrbanTerrainClearables.isWaterBody(existing, options)) {
                        return null;
                    }
                    if (!UrbanTerrainClearables.isPaveable(existing, overwritable, options)
                        && !RoadConnectionPolisher.isRoadFamily(existing, connections)) {
                        return null;
                    }
                    if (connection == null) {
                        return roadId;
                    }
                    final String picked = connection.pickTerrainForStep(
                        fromX, fromY, toX, toY, existing, options
                    );
                    return OrthogonalPathCarver.resolveTerrainId(picked, roadId, registry);
                },
                overwritable
            );
        }

        if (!UrbanTerrainClearables.isPaveable(grid.getOmtId(x, y), overwritable, options)) {
            return 0;
        }
        grid.setOmtId(x, y, roadId);
        return 1;
    }

    private static int[] findNearestRoad(
        final OvermapGrid grid,
        final int originX,
        final int originY,
        final OvermapConnectionRegistry connections,
        final CityBuildingDefinition building,
        final int anchorX,
        final int anchorY
    ) {
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int r = 1; r <= SEARCH_RADIUS; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) {
                        continue;
                    }
                    final int x = originX + dx;
                    final int y = originY + dy;
                    if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
                        continue;
                    }
                    if (building != null && isInsideFootprint(building, anchorX, anchorY, x, y)) {
                        continue;
                    }
                    if (!RoadConnectionPolisher.isRoadFamily(grid.getOmtId(x, y), connections)) {
                        continue;
                    }
                    final int dist = Math.abs(dx) + Math.abs(dy);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new int[] { x, y };
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static boolean isInsideFootprint(
        final CityBuildingDefinition building,
        final int anchorX,
        final int anchorY,
        final int x,
        final int y
    ) {
        final BuildingFootprint footprint = BuildingFootprint.atZ(building, 0);
        for (final CityBuildingPiece piece : footprint.getPieces()) {
            if (anchorX + piece.getOffsetX() == x && anchorY + piece.getOffsetY() == y) {
                return true;
            }
        }
        return false;
    }

    private static int[] rotateOffset(final int ox, final int oy, final int quarterTurnsClockwise) {
        int x = ox;
        int y = oy;
        final int turns = Math.floorMod(quarterTurnsClockwise, 4);
        for (int i = 0; i < turns; i++) {
            final int nx = -y;
            final int ny = x;
            x = nx;
            y = ny;
        }
        return new int[] { x, y };
    }

    private static OvermapConnectionDefinition resolveConnection(
        final OvermapConnectionRegistry connections,
        final String connectionId
    ) {
        if (connections == null || connectionId == null || connectionId.isEmpty()) {
            return null;
        }
        final OvermapConnectionDefinition found = connections.find(connectionId).orElse(null);
        if (found != null) {
            return found;
        }
        if ("local_road".equals(connectionId)) {
            return connections.find("test_local_road").orElse(null);
        }
        return null;
    }
}
