package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.OmTerrainGrid;

import java.util.List;
import java.util.Optional;

/** Finds one json mapgen whose nested {@code om_terrain} grid covers a full floor (BN multitile). */
public final class CombinedFloorMapgenResolver {

    private CombinedFloorMapgenResolver() {}

    public static Optional<CombinedFloorMatch> resolve(
        final MapgenCatalog catalog,
        final List<CityBuildingPiece> pieces
    ) {
        if (catalog == null || pieces == null || pieces.isEmpty()) {
            return Optional.empty();
        }

        for (final JsonMapgenDefinition definition : catalog.runnableOnly()) {
            if (!definition.getOmTerrainGrid().isPresent()) {
                continue;
            }
            final OmTerrainGrid grid = definition.getOmTerrainGrid().get();
            if (!gridMatchesPieces(pieces, grid)) {
                continue;
            }
            return Optional.of(new CombinedFloorMatch(
                definition,
                OmtStitchComposer.layoutPieceRects(pieces, OmtStitchComposer.DEFAULT_OMT_SIZE)
            ));
        }
        return Optional.empty();
    }

    private static boolean gridMatchesPieces(final List<CityBuildingPiece> pieces, final OmTerrainGrid grid) {
        final int minX = minOffsetX(pieces);
        final int maxX = maxOffsetX(pieces);
        final int minY = minOffsetY(pieces);
        final int maxY = maxOffsetY(pieces);
        final int footprintW = maxX - minX + 1;
        final int footprintH = maxY - minY + 1;
        if (grid.width() != footprintW || grid.height() != footprintH) {
            return false;
        }
        return gridMatchesPiecesWithAxis(pieces, grid, minX, maxX, minY, maxY, false)
            || gridMatchesPiecesWithAxis(pieces, grid, minX, maxX, minY, maxY, true);
    }

    private static boolean gridMatchesPiecesWithAxis(
        final List<CityBuildingPiece> pieces,
        final OmTerrainGrid grid,
        final int minX,
        final int maxX,
        final int minY,
        final int maxY,
        final boolean flip
    ) {
        for (final CityBuildingPiece piece : pieces) {
            final String expected = OvermapTerrainResolver.stripRotation(piece.getOvermapId());
            final int col = flip
                ? maxX - piece.getOffsetX()
                : piece.getOffsetX() - minX;
            final int row = flip
                ? maxY - piece.getOffsetY()
                : piece.getOffsetY() - minY;
            if (col < 0 || row < 0 || col >= grid.width() || row >= grid.height()) {
                return false;
            }
            if (!expected.equals(grid.get(row, col))) {
                return false;
            }
        }
        return true;
    }

    private static int minOffsetX(final List<CityBuildingPiece> pieces) {
        int min = Integer.MAX_VALUE;
        for (final CityBuildingPiece piece : pieces) {
            min = Math.min(min, piece.getOffsetX());
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int minOffsetY(final List<CityBuildingPiece> pieces) {
        int min = Integer.MAX_VALUE;
        for (final CityBuildingPiece piece : pieces) {
            min = Math.min(min, piece.getOffsetY());
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int maxOffsetX(final List<CityBuildingPiece> pieces) {
        int max = 0;
        for (final CityBuildingPiece piece : pieces) {
            max = Math.max(max, piece.getOffsetX());
        }
        return max;
    }

    private static int maxOffsetY(final List<CityBuildingPiece> pieces) {
        int max = 0;
        for (final CityBuildingPiece piece : pieces) {
            max = Math.max(max, piece.getOffsetY());
        }
        return max;
    }

    public static final class CombinedFloorMatch {
        private final JsonMapgenDefinition definition;
        private final List<OmtPieceRect> pieceRects;

        public CombinedFloorMatch(
            final JsonMapgenDefinition definition,
            final List<OmtPieceRect> pieceRects
        ) {
            this.definition = definition;
            this.pieceRects = pieceRects;
        }

        public JsonMapgenDefinition getDefinition() {
            return definition;
        }

        public List<OmtPieceRect> getPieceRects() {
            return pieceRects;
        }
    }
}
