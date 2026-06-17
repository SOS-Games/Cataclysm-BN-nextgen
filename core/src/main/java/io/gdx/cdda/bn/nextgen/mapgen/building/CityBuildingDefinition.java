package io.gdx.cdda.bn.nextgen.mapgen.building;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/** Parsed {@code type: city_building} with OMT layout metadata (P5). */
public final class CityBuildingDefinition {

    private final String id;
    private final Path sourceFile;
    private final List<CityBuildingPiece> pieces;

    public CityBuildingDefinition(
        final String id,
        final Path sourceFile,
        final List<CityBuildingPiece> pieces
    ) {
        this.id = id;
        this.sourceFile = sourceFile;
        this.pieces = Collections.unmodifiableList(new ArrayList<>(pieces));
    }

    public String getId() {
        return id;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public List<CityBuildingPiece> getPieces() {
        return pieces;
    }

    public List<Integer> distinctZLevels() {
        final TreeSet<Integer> levels = new TreeSet<>();
        for (final CityBuildingPiece piece : pieces) {
            levels.add(piece.getZLevel());
        }
        return Collections.unmodifiableList(new ArrayList<>(levels));
    }

    public List<CityBuildingPiece> piecesAtZ(final int zLevel) {
        final List<CityBuildingPiece> atZ = new ArrayList<>();
        for (final CityBuildingPiece piece : pieces) {
            if (piece.getZLevel() == zLevel) {
                atZ.add(piece);
            }
        }
        return Collections.unmodifiableList(atZ);
    }

    public boolean isMultiTileAtZ(final int zLevel) {
        final List<CityBuildingPiece> atZ = piecesAtZ(zLevel);
        if (atZ.size() > 1) {
            return true;
        }
        if (atZ.isEmpty()) {
            return false;
        }
        final CityBuildingPiece piece = atZ.get(0);
        return piece.getOffsetX() != 0 || piece.getOffsetY() != 0;
    }

    public int floorCount() {
        return distinctZLevels().size();
    }

    public boolean hasMultiTileLayout() {
        for (final int zLevel : distinctZLevels()) {
            if (isMultiTileAtZ(zLevel)) {
                return true;
            }
        }
        return false;
    }

    /** Multi-floor or multi-piece — import as a building bundle instead of single mapgen. */
    public boolean isBundledBuilding() {
        return floorCount() > 1 || hasMultiTileLayout();
    }

    public String buildingSummaryLabel() {
        if (!isBundledBuilding()) {
            return "";
        }
        final StringBuilder label = new StringBuilder();
        if (floorCount() > 1) {
            label.append(floorCount()).append(" floors");
        }
        if (hasMultiTileLayout()) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(multiTileLabel());
        }
        return label.toString();
    }

    public String multiTileLabel() {
        if (!hasMultiTileLayout()) {
            return "";
        }
        return "multi-tile " + omtFootprintWidth() + "×" + omtFootprintHeight();
    }

    public int omtFootprintWidth() {
        int max = 0;
        for (final CityBuildingPiece piece : pieces) {
            max = Math.max(max, piece.getOffsetX() + 1);
        }
        return max;
    }

    public int omtFootprintHeight() {
        int max = 0;
        for (final CityBuildingPiece piece : pieces) {
            max = Math.max(max, piece.getOffsetY() + 1);
        }
        return max;
    }

    public String omtFootprintLabel() {
        return multiTileLabel();
    }
}
