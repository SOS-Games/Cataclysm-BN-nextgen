package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.OmTerrainGrid;

import java.util.Optional;

/** Blits multitile json mapgen onto a shared floor canvas at BN OMT coordinates. */
public final class OmTerrainMapgenPlacer {

    private OmTerrainMapgenPlacer() {}

    public static MapGrid placeOnCanvas(
        final MapGrid source,
        final OmTerrainGrid sourceOmGrid,
        final String overmapId,
        final OmTerrainGrid referenceOmGrid,
        final int buildingOffsetX,
        final int buildingOffsetY,
        final int canvasWidth,
        final int canvasHeight,
        final String fillTer
    ) {
        if (source == null || sourceOmGrid == null || referenceOmGrid == null) {
            throw new IllegalArgumentException("source, sourceOmGrid, and referenceOmGrid are required");
        }
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            throw new IllegalArgumentException("canvas dimensions must be > 0");
        }

        final int omtCellW = canvasWidth / referenceOmGrid.width();
        final int omtCellH = canvasHeight / referenceOmGrid.height();
        if (omtCellW <= 0 || omtCellH <= 0) {
            throw new IllegalArgumentException("reference canvas is too small for om_terrain grid");
        }

        final Optional<GridCell> cell = findCell(sourceOmGrid, overmapId);
        final int destX = (buildingOffsetX + cell.map(value -> value.col).orElse(0)) * omtCellW;
        final int destY = (buildingOffsetY + cell.map(value -> value.row).orElse(0)) * omtCellH;

        final MapGrid canvas = new MapGrid(canvasWidth, canvasHeight, fillTer);
        canvas.blitFrom(source, destX, destY, fillTer);
        return canvas;
    }

    public static Optional<PlacementRect> blitAtReferenceCell(
        final MapGrid canvas,
        final MapGrid source,
        final OmTerrainGrid sourceOmGrid,
        final String overmapId,
        final OmTerrainGrid referenceOmGrid,
        final int anchorCol,
        final int anchorRow,
        final String unsetFillTer
    ) {
        if (canvas == null || source == null || sourceOmGrid == null || referenceOmGrid == null) {
            throw new IllegalArgumentException("canvas, source, sourceOmGrid, and referenceOmGrid are required");
        }

        final int omtCellW = canvas.width() / referenceOmGrid.width();
        final int omtCellH = canvas.height() / referenceOmGrid.height();
        if (omtCellW <= 0 || omtCellH <= 0) {
            return Optional.empty();
        }

        final Optional<GridCell> cell = findCell(sourceOmGrid, overmapId);
        final int destX = (anchorCol + cell.map(value -> value.col).orElse(0)) * omtCellW;
        final int destY = (anchorRow + cell.map(value -> value.row).orElse(0)) * omtCellH;
        canvas.blitFrom(source, destX, destY, unsetFillTer);
        return Optional.of(new PlacementRect(destX, destY, source.width(), source.height()));
    }

    public static Optional<GridCell> findCell(final OmTerrainGrid grid, final String overmapId) {
        final Optional<GridCell> exact = findCellMatching(grid, overmapId);
        if (exact.isPresent()) {
            return exact;
        }
        final String stripped = OvermapTerrainResolver.stripRotation(overmapId);
        if (!stripped.equals(overmapId)) {
            return findCellMatching(grid, stripped);
        }
        return Optional.empty();
    }

    private static Optional<GridCell> findCellMatching(final OmTerrainGrid grid, final String target) {
        for (int row = 0; row < grid.height(); row++) {
            for (int col = 0; col < grid.width(); col++) {
                final String cellId = grid.get(row, col);
                if (target.equals(cellId) || target.equals(OvermapTerrainResolver.stripRotation(cellId))) {
                    return Optional.of(new GridCell(row, col));
                }
            }
        }
        return Optional.empty();
    }

    public static final class GridCell {
        public final int row;
        public final int col;

        public GridCell(final int row, final int col) {
            this.row = row;
            this.col = col;
        }
    }

    public static final class PlacementRect {
        public final int destX;
        public final int destY;
        public final int width;
        public final int height;

        public PlacementRect(final int destX, final int destY, final int width, final int height) {
            this.destX = destX;
            this.destY = destY;
            this.width = width;
            this.height = height;
        }
    }
}
