package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.json.OmTerrainGrid;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Crop / rotate / translate spawn markers alongside stitched OMT mapgen. */
public final class SpawnMarkerTransform {

    private SpawnMarkerTransform() {}

    public static List<SpawnMarker> rotate(
        final List<SpawnMarker> markers,
        final int gridWidth,
        final int gridHeight,
        final int quarterTurnsClockwise
    ) {
        if (markers == null || markers.isEmpty()) {
            return List.of();
        }
        if (Math.floorMod(quarterTurnsClockwise, 4) == 0) {
            return List.copyOf(markers);
        }
        final List<SpawnMarker> rotated = new ArrayList<>(markers.size());
        for (final SpawnMarker marker : markers) {
            final int[] point = MapGridRotator.rotatePointClockwise(
                marker.x,
                marker.y,
                gridWidth,
                gridHeight,
                quarterTurnsClockwise
            );
            rotated.add(reposition(marker, point[0], point[1]));
        }
        return rotated;
    }

    public static List<SpawnMarker> translate(
        final List<SpawnMarker> markers,
        final int dx,
        final int dy
    ) {
        if (markers == null || markers.isEmpty() || (dx == 0 && dy == 0)) {
            return markers == null ? List.of() : List.copyOf(markers);
        }
        final List<SpawnMarker> moved = new ArrayList<>(markers.size());
        for (final SpawnMarker marker : markers) {
            moved.add(reposition(marker, marker.x + dx, marker.y + dy));
        }
        return moved;
    }

    public static List<SpawnMarker> crop(
        final List<SpawnMarker> markers,
        final int regionX,
        final int regionY,
        final int regionWidth,
        final int regionHeight
    ) {
        if (markers == null || markers.isEmpty()) {
            return List.of();
        }
        final List<SpawnMarker> kept = new ArrayList<>();
        for (final SpawnMarker marker : markers) {
            if (marker.x < regionX || marker.y < regionY) {
                continue;
            }
            if (marker.x >= regionX + regionWidth || marker.y >= regionY + regionHeight) {
                continue;
            }
            kept.add(reposition(marker, marker.x - regionX, marker.y - regionY));
        }
        return kept;
    }

    public static List<SpawnMarker> orientForStitchPiece(
        final List<SpawnMarker> sourceMarkers,
        final MapGrid sourceGrid,
        final OmTerrainGrid sourceOmGrid,
        final String overmapId,
        final int destX,
        final int destY
    ) {
        if (sourceMarkers == null || sourceMarkers.isEmpty()) {
            return List.of();
        }
        final List<SpawnMarker> oriented;
        if (sourceOmGrid == null) {
            oriented = sourceMarkers;
        } else {
            final Optional<OmTerrainMapgenPlacer.GridCell> cell =
                OmTerrainMapgenPlacer.findCell(sourceOmGrid, overmapId);
            if (!cell.isPresent()) {
                return List.of();
            }
            final int omtW = sourceGrid.width() / sourceOmGrid.width();
            final int omtH = sourceGrid.height() / sourceOmGrid.height();
            if (omtW <= 0 || omtH <= 0) {
                return List.of();
            }
            final int cropX = cell.get().col * omtW;
            final int cropY = cell.get().row * omtH;
            final List<SpawnMarker> cropped = crop(sourceMarkers, cropX, cropY, omtW, omtH);
            final int rotation = OmTerrainMapgenPlacer.suffixRotationForPiece(sourceOmGrid, overmapId);
            oriented = rotate(cropped, omtW, omtH, rotation);
        }
        return translate(oriented, destX, destY);
    }

    private static SpawnMarker reposition(final SpawnMarker marker, final int x, final int y) {
        return new SpawnMarker(marker.kind, marker.groupId, marker.displayName, x, y, marker.density);
    }
}
