package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Formats building OMT piece layout for clipboard debug reports. */
public final class BuildingPieceDebugFormatter {

    private static final int OMT_CELLS = 24;

    private BuildingPieceDebugFormatter() {}

    public static String format(final MapVolume volume, final CityBuildingDefinition building) {
        if (volume == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder();
        out.append("building-piece-debug\n");
        out.append("building: ").append(volume.getBuildingId()).append('\n');
        if (building != null && building.getSourceFile() != null) {
            out.append("source: ").append(building.getSourceFile()).append('\n');
        }
        if (building != null) {
            out.append("layout: ").append(building.getLayoutKind()).append('\n');
        }

        for (final int zLevel : volume.getZLevels()) {
            appendFloor(out, volume, building, zLevel);
        }
        return out.toString().trim();
    }

    private static void appendFloor(
        final StringBuilder out,
        final MapVolume volume,
        final CityBuildingDefinition building,
        final int zLevel
    ) {
        out.append('\n').append("[z=").append(zLevel).append("]\n");
        final MapGrid floorGrid = volume.getGridAtZ(zLevel);
        if (floorGrid != null) {
            out.append("submap_grid: ").append(floorGrid.width()).append('x').append(floorGrid.height()).append('\n');
        }

        final List<CityBuildingPiece> omtPieces = building == null
            ? Collections.<CityBuildingPiece>emptyList()
            : building.piecesAtZ(zLevel);
        if (!omtPieces.isEmpty()) {
            appendOmtGrid(out, omtPieces);
            appendBnPoints(out, omtPieces);
        }

        final List<OmtPieceRect> stitched = volume.getPieceLayoutsAtZ(zLevel);
        if (!stitched.isEmpty()) {
            appendStitchedChunks(out, stitched);
            if (omtPieces.isEmpty()) {
                appendStitchedOmtGrid(out, stitched);
            }
        }

        if (omtPieces.isEmpty() && stitched.isEmpty()) {
            out.append("(no multi-chunk piece layout on this floor)\n");
        }
    }

    private static void appendOmtGrid(final StringBuilder out, final List<CityBuildingPiece> pieces) {
        final Bounds bounds = boundsForOmtPieces(pieces);
        out.append("omt grid ").append(bounds.width()).append('x').append(bounds.height())
            .append(" (x→, y↓)\n");

        final int columnWidth = maxIdWidth(pieces, bounds) + 2;
        out.append("     ");
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            out.append(pad("x" + x, columnWidth));
        }
        out.append('\n');

        for (int y = bounds.minY; y <= bounds.maxY; y++) {
            out.append("y").append(y).append(' ');
            for (int x = bounds.minX; x <= bounds.maxX; x++) {
                final String cell = findOmtPiece(pieces, x, y).map(CityBuildingPiece::getOvermapId).orElse(".");
                out.append(pad(cell, columnWidth));
            }
            out.append('\n');
        }
    }

    private static void appendBnPoints(final StringBuilder out, final List<CityBuildingPiece> pieces) {
        out.append("bn overmaps points:\n");
        final List<CityBuildingPiece> sorted = new ArrayList<>(pieces);
        sorted.sort(Comparator
            .comparingInt(CityBuildingPiece::getOffsetY)
            .thenComparingInt(CityBuildingPiece::getOffsetX)
            .thenComparing(CityBuildingPiece::getOvermapId));
        for (final CityBuildingPiece piece : sorted) {
            out.append("  { \"overmap\": \"").append(piece.getOvermapId()).append("\", \"point\": [")
                .append(piece.getOffsetX()).append(", ")
                .append(piece.getOffsetY()).append(", ")
                .append(piece.getZLevel()).append("] }\n");
        }
    }

    private static void appendStitchedChunks(final StringBuilder out, final List<OmtPieceRect> layouts) {
        out.append("stitched chunks (submap cells, not per-tile):\n");
        final List<OmtPieceRect> sorted = new ArrayList<>(layouts);
        sorted.sort(Comparator
            .comparingInt(OmtPieceRect::getOriginY)
            .thenComparingInt(OmtPieceRect::getOriginX)
            .thenComparing(OmtPieceRect::getOvermapId));
        for (final OmtPieceRect piece : sorted) {
            out.append("  ").append(piece.getOvermapId())
                .append("  origin=(").append(piece.getOriginX()).append(',').append(piece.getOriginY()).append(')')
                .append("  size=").append(piece.getWidth()).append('x').append(piece.getHeight());
            if (piece.getWidth() % OMT_CELLS == 0 && piece.getHeight() % OMT_CELLS == 0) {
                out.append("  omt=(")
                    .append(piece.getOriginX() / OMT_CELLS).append(',')
                    .append(piece.getOriginY() / OMT_CELLS).append(')');
            }
            out.append('\n');
        }
    }

    private static void appendStitchedOmtGrid(final StringBuilder out, final List<OmtPieceRect> layouts) {
        final Map<String, String> cellIds = new HashMap<>();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final OmtPieceRect piece : layouts) {
            if (piece.getWidth() != OMT_CELLS || piece.getHeight() != OMT_CELLS) {
                return;
            }
            final int omtX = piece.getOriginX() / OMT_CELLS;
            final int omtY = piece.getOriginY() / OMT_CELLS;
            cellIds.put(omtX + "," + omtY, piece.getOvermapId());
            minX = Math.min(minX, omtX);
            maxX = Math.max(maxX, omtX);
            minY = Math.min(minY, omtY);
            maxY = Math.max(maxY, omtY);
        }

        final int columnWidth = maxStitchedIdWidth(cellIds) + 2;
        out.append("omt grid from stitch ").append(maxX - minX + 1).append('x').append(maxY - minY + 1)
            .append(" (x→, y↓)\n");
        out.append("     ");
        for (int x = minX; x <= maxX; x++) {
            out.append(pad("x" + x, columnWidth));
        }
        out.append('\n');
        for (int y = minY; y <= maxY; y++) {
            out.append("y").append(y).append(' ');
            for (int x = minX; x <= maxX; x++) {
                final String id = cellIds.getOrDefault(x + "," + y, ".");
                out.append(pad(id, columnWidth));
            }
            out.append('\n');
        }
    }

    private static Optional<CityBuildingPiece> findOmtPiece(
        final List<CityBuildingPiece> pieces,
        final int offsetX,
        final int offsetY
    ) {
        for (final CityBuildingPiece piece : pieces) {
            if (piece.getOffsetX() == offsetX && piece.getOffsetY() == offsetY) {
                return Optional.of(piece);
            }
        }
        return Optional.empty();
    }

    private static Bounds boundsForOmtPieces(final List<CityBuildingPiece> pieces) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (final CityBuildingPiece piece : pieces) {
            minX = Math.min(minX, piece.getOffsetX());
            maxX = Math.max(maxX, piece.getOffsetX());
            minY = Math.min(minY, piece.getOffsetY());
            maxY = Math.max(maxY, piece.getOffsetY());
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private static int maxIdWidth(final List<CityBuildingPiece> pieces, final Bounds bounds) {
        int max = 1;
        for (int y = bounds.minY; y <= bounds.maxY; y++) {
            for (int x = bounds.minX; x <= bounds.maxX; x++) {
                final String id = findOmtPiece(pieces, x, y).map(CityBuildingPiece::getOvermapId).orElse(".");
                max = Math.max(max, id.length());
            }
        }
        return max;
    }

    private static int maxStitchedIdWidth(final Map<String, String> cellIds) {
        int max = 1;
        for (final String id : cellIds.values()) {
            max = Math.max(max, id.length());
        }
        return max;
    }

    private static String pad(final String text, final int width) {
        if (text.length() >= width) {
            return text + ' ';
        }
        final StringBuilder padded = new StringBuilder(text);
        while (padded.length() < width) {
            padded.append(' ');
        }
        return padded.toString();
    }

    private static final class Bounds {
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;

        private Bounds(final int minX, final int minY, final int maxX, final int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }
    }
}
