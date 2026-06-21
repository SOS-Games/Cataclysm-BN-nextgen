package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
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
        final int destOmtCol = buildingOffsetX + cell.map(value -> value.col).orElse(0);
        final int destOmtRow = buildingOffsetY + cell.map(value -> value.row).orElse(0);

        final MapGrid canvas = new MapGrid(canvasWidth, canvasHeight, fillTer);
        blitAtReferenceCell(
            canvas,
            source,
            sourceOmGrid,
            overmapId,
            referenceOmGrid,
            destOmtCol,
            destOmtRow,
            fillTer
        );
        return canvas;
    }

    public static Optional<PlacementRect> blitAtReferenceCell(
        final MapGrid canvas,
        final MapGrid source,
        final OmTerrainGrid sourceOmGrid,
        final String overmapId,
        final OmTerrainGrid referenceOmGrid,
        final int destOmtCol,
        final int destOmtRow,
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
        if (!cell.isPresent()) {
            return Optional.empty();
        }

        final int destX = destOmtCol * omtCellW;
        final int destY = destOmtRow * omtCellH;
        final Optional<MapGrid> oriented = extractOrientedOmtSubmap(source, sourceOmGrid, overmapId);
        if (!oriented.isPresent()) {
            return Optional.empty();
        }
        final MapGrid pieceGrid = oriented.get();
        canvas.blitFrom(pieceGrid, 0, 0, pieceGrid.width(), pieceGrid.height(), destX, destY, unsetFillTer);
        return Optional.of(new PlacementRect(destX, destY, pieceGrid.width(), pieceGrid.height()));
    }

    /** Crops one OMT slot from multitile mapgen, then applies {@code _north}/… suffix rotation. */
    public static Optional<MapGrid> extractOrientedOmtSubmap(
        final MapGrid source,
        final OmTerrainGrid sourceOmGrid,
        final String overmapId
    ) {
        if (source == null) {
            return Optional.empty();
        }
        if (sourceOmGrid == null) {
            return Optional.of(source);
        }
        final Optional<GridCell> cell = findCell(sourceOmGrid, overmapId);
        if (!cell.isPresent()) {
            return Optional.empty();
        }
        final int omtW = source.width() / sourceOmGrid.width();
        final int omtH = source.height() / sourceOmGrid.height();
        if (omtW <= 0 || omtH <= 0) {
            return Optional.empty();
        }
        final MapGrid cropped = copyRegion(
            source,
            cell.get().col * omtW,
            cell.get().row * omtH,
            omtW,
            omtH
        );
        final int rotation = suffixRotationForPiece(sourceOmGrid, overmapId);
        return Optional.of(MapGridRotator.rotate(cropped, rotation));
    }

    public static int suffixRotationForPiece(final OmTerrainGrid grid, final String overmapId) {
        return omTerrainGridContainsExactId(grid, overmapId)
            ? 0
            : MapGridRotator.rotationFromOmSuffix(overmapId);
    }

    private static boolean omTerrainGridContainsExactId(final OmTerrainGrid grid, final String overmapId) {
        for (int row = 0; row < grid.height(); row++) {
            for (int col = 0; col < grid.width(); col++) {
                if (overmapId.equals(grid.get(row, col))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static MapGrid copyRegion(
        final MapGrid source,
        final int sourceX,
        final int sourceY,
        final int copyWidth,
        final int copyHeight
    ) {
        final MapGrid region = new MapGrid(copyWidth, copyHeight, source.getDefaultTerrainId());
        for (int y = 0; y < copyHeight; y++) {
            for (int x = 0; x < copyWidth; x++) {
                final int srcX = sourceX + x;
                final int srcY = sourceY + y;
                region.setTerrain(x, y, source.get(srcX, srcY).getTerrainId());
                final String furnitureId = source.get(srcX, srcY).getFurnitureId();
                if (furnitureId != null && !furnitureId.isEmpty()) {
                    region.setFurniture(x, y, furnitureId);
                }
            }
        }
        return region;
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
