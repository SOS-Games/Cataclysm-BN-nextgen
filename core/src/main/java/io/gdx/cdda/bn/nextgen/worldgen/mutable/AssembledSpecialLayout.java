package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Runtime layout assembled from a {@link MutableSpecialDefinition} (W6). */
public final class AssembledSpecialLayout {

    private final String specialId;
    private final List<PlacedMutablePiece> pieces;

    public AssembledSpecialLayout(final String specialId, final List<PlacedMutablePiece> pieces) {
        this.specialId = specialId;
        this.pieces = pieces == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(pieces));
    }

    public String getSpecialId() {
        return specialId;
    }

    public List<PlacedMutablePiece> getPieces() {
        return pieces;
    }

    public int getWidth() {
        int minX = 0;
        int maxX = 0;
        boolean first = true;
        for (final PlacedMutablePiece piece : pieces) {
            if (first) {
                minX = piece.getOffsetX();
                maxX = piece.getOffsetX();
                first = false;
                continue;
            }
            minX = Math.min(minX, piece.getOffsetX());
            maxX = Math.max(maxX, piece.getOffsetX());
        }
        return first ? 0 : maxX - minX + 1;
    }

    public int getHeight() {
        int minY = 0;
        int maxY = 0;
        boolean first = true;
        for (final PlacedMutablePiece piece : pieces) {
            if (first) {
                minY = piece.getOffsetY();
                maxY = piece.getOffsetY();
                first = false;
                continue;
            }
            minY = Math.min(minY, piece.getOffsetY());
            maxY = Math.max(maxY, piece.getOffsetY());
        }
        return first ? 0 : maxY - minY + 1;
    }

    public int getMinOffsetX() {
        int minX = 0;
        boolean first = true;
        for (final PlacedMutablePiece piece : pieces) {
            if (first) {
                minX = piece.getOffsetX();
                first = false;
                continue;
            }
            minX = Math.min(minX, piece.getOffsetX());
        }
        return minX;
    }

    public int getMinOffsetY() {
        int minY = 0;
        boolean first = true;
        for (final PlacedMutablePiece piece : pieces) {
            if (first) {
                minY = piece.getOffsetY();
                first = false;
                continue;
            }
            minY = Math.min(minY, piece.getOffsetY());
        }
        return minY;
    }
}
