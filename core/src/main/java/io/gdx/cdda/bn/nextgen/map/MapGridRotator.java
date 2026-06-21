package io.gdx.cdda.bn.nextgen.map;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Quarter-turn rotation for {@link MapGrid} ter/furn layers (P14). */
public final class MapGridRotator {

    private static final List<String> DIAGONAL_SUFFIXES = Arrays.asList(
        "_north_east",
        "_north_west",
        "_south_east",
        "_south_west"
    );

    private MapGridRotator() {}

    public static MapGrid rotate(final MapGrid source, final int quarterTurnsClockwise) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        final int turns = Math.floorMod(quarterTurnsClockwise, 4);
        if (turns == 0) {
            return source;
        }
        MapGrid current = source;
        for (int i = 0; i < turns; i++) {
            current = rotate90Clockwise(current);
        }
        return current;
    }

    public static int rotationFromOmSuffix(final String overmapId) {
        if (overmapId == null || overmapId.isEmpty()) {
            return 0;
        }
        final String lower = overmapId.toLowerCase(Locale.ROOT);
        for (final String suffix : DIAGONAL_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return 0;
            }
        }
        if (lower.endsWith("_east")) {
            return 1;
        }
        if (lower.endsWith("_south")) {
            return 2;
        }
        if (lower.endsWith("_west")) {
            return 3;
        }
        return 0;
    }

    /** Runner rotation when mapgen will be cropped per OMT slot (rotation applied after crop). */
    public static int runnerOmtRotation(final boolean cropFromMultitileOmGrid, final String overmapId) {
        return cropFromMultitileOmGrid ? 0 : rotationFromOmSuffix(overmapId);
    }

    public static int rotationForBuildingPiece(
        final String overmapId,
        final boolean combinedMultitileMapgen
    ) {
        if (combinedMultitileMapgen) {
            return 0;
        }
        return rotationFromOmSuffix(overmapId);
    }

  /** Maps a cell coordinate through {@code quarterTurnsClockwise} on a {@code width}×{@code height} grid. */
    public static int[] rotatePointClockwise(
        final int x,
        final int y,
        final int width,
        final int height,
        final int quarterTurnsClockwise
    ) {
        int px = x;
        int py = y;
        int w = width;
        int h = height;
        final int turns = Math.floorMod(quarterTurnsClockwise, 4);
        for (int i = 0; i < turns; i++) {
            final int newX = h - 1 - py;
            final int newY = px;
            px = newX;
            py = newY;
            final int swap = w;
            w = h;
            h = swap;
        }
        return new int[] { px, py };
    }

    private static MapGrid rotate90Clockwise(final MapGrid source) {
        final int width = source.width();
        final int height = source.height();
        final MapGrid rotated = new MapGrid(height, width, source.getDefaultTerrainId());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int newX = height - 1 - y;
                final int newY = x;
                copyCell(source, x, y, rotated, newX, newY);
            }
        }
        return rotated;
    }

    private static void copyCell(
        final MapGrid source,
        final int sourceX,
        final int sourceY,
        final MapGrid dest,
        final int destX,
        final int destY
    ) {
        final MapCell cell = source.get(sourceX, sourceY);
        dest.setTerrain(destX, destY, cell.getTerrainId());
        final String furnitureId = cell.getFurnitureId();
        if (furnitureId != null && !furnitureId.isEmpty()) {
            dest.setFurniture(destX, destY, furnitureId);
        }
    }
}
