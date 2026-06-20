package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;

import java.util.List;

/** Ground-floor OMT footprint for a building bundle (W4). */
public final class BuildingFootprint {

    private final int minOffsetX;
    private final int minOffsetY;
    private final int width;
    private final int height;
    private final List<CityBuildingPiece> pieces;

    private BuildingFootprint(
        final int minOffsetX,
        final int minOffsetY,
        final int width,
        final int height,
        final List<CityBuildingPiece> pieces
    ) {
        this.minOffsetX = minOffsetX;
        this.minOffsetY = minOffsetY;
        this.width = width;
        this.height = height;
        this.pieces = pieces;
    }

    public static BuildingFootprint atZ(final CityBuildingDefinition building, final int zLevel) {
        final List<CityBuildingPiece> pieces = building.piecesAtZ(zLevel);
        int minX = 0;
        int minY = 0;
        int maxX = 0;
        int maxY = 0;
        boolean first = true;
        for (final CityBuildingPiece piece : pieces) {
            if (first) {
                minX = piece.getOffsetX();
                minY = piece.getOffsetY();
                maxX = piece.getOffsetX();
                maxY = piece.getOffsetY();
                first = false;
                continue;
            }
            minX = Math.min(minX, piece.getOffsetX());
            minY = Math.min(minY, piece.getOffsetY());
            maxX = Math.max(maxX, piece.getOffsetX());
            maxY = Math.max(maxY, piece.getOffsetY());
        }
        if (pieces.isEmpty()) {
            return new BuildingFootprint(0, 0, 0, 0, pieces);
        }
        return new BuildingFootprint(
            minX,
            minY,
            maxX - minX + 1,
            maxY - minY + 1,
            pieces
        );
    }

    public int getMinOffsetX() {
        return minOffsetX;
    }

    public int getMinOffsetY() {
        return minOffsetY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<CityBuildingPiece> getPieces() {
        return pieces;
    }

    public boolean isEmpty() {
        return pieces.isEmpty() || width <= 0 || height <= 0;
    }
}
