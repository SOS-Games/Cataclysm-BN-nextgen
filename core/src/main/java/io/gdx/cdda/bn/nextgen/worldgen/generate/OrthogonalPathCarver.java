package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Orthogonal path carving shared by rivers and roads (W5). */
final class OrthogonalPathCarver {

    private OrthogonalPathCarver() {}

    static List<int[]> buildPath(
        final int startX,
        final int startY,
        final int endX,
        final int endY,
        final Random rng
    ) {
        final List<int[]> path = new ArrayList<>();
        int x = startX;
        int y = startY;
        path.add(new int[] { x, y });
        int guard = 4096;
        while ((x != endX || y != endY) && guard-- > 0) {
            final boolean moveX = x != endX && (y == endY || rng == null || rng.nextBoolean());
            if (moveX) {
                x += Integer.compare(endX, x);
            } else if (y != endY) {
                y += Integer.compare(endY, y);
            } else {
                x += Integer.compare(endX, x);
            }
            path.add(new int[] { x, y });
        }
        return path;
    }

    static int paintPath(
        final OvermapGrid grid,
        final List<int[]> path,
        final String terrainId,
        final Set<String> overwritableIds
    ) {
        if (grid == null || path == null || path.isEmpty() || terrainId == null || terrainId.isEmpty()) {
            return 0;
        }
        int painted = 0;
        for (final int[] cell : path) {
            final int x = cell[0];
            final int y = cell[1];
            if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
                continue;
            }
            if (!overwritableIds.contains(grid.getOmtId(x, y))) {
                continue;
            }
            grid.setOmtId(x, y, terrainId);
            painted++;
        }
        return painted;
    }

    static int paintDirectionalPath(
        final OvermapGrid grid,
        final List<int[]> path,
        final StepTerrainResolver resolver,
        final Set<String> overwritableIds
    ) {
        if (grid == null || path == null || path.isEmpty() || resolver == null) {
            return 0;
        }
        int painted = 0;
        int prevX = path.get(0)[0];
        int prevY = path.get(0)[1];
        for (int i = 0; i < path.size(); i++) {
            final int x = path.get(i)[0];
            final int y = path.get(i)[1];
            if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
                prevX = x;
                prevY = y;
                continue;
            }
            final String existing = grid.getOmtId(x, y);
            final String terrainId = resolver.resolve(prevX, prevY, x, y, existing);
            if (terrainId == null || terrainId.isEmpty()) {
                prevX = x;
                prevY = y;
                continue;
            }
            final boolean riverBridge = existing != null && !overwritableIds.contains(existing);
            if (!riverBridge && !overwritableIds.contains(existing)) {
                prevX = x;
                prevY = y;
                continue;
            }
            grid.setOmtId(x, y, terrainId);
            painted++;
            prevX = x;
            prevY = y;
        }
        return painted;
    }

    interface StepTerrainResolver {
        String resolve(int fromX, int fromY, int toX, int toY, String existingOmtId);
    }

    static Set<String> terrainOverwritableIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final String... extraIds
    ) {
        final Set<String> ids = new HashSet<>(OmtBuildingBlitter.defaultClearableIds(options, registry));
        if (extraIds != null) {
            for (final String extraId : extraIds) {
                if (extraId != null && !extraId.isEmpty()) {
                    ids.add(extraId);
                }
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    static String resolveTerrainId(
        final String preferred,
        final String fallback,
        final OvermapTerrainRegistry registry
    ) {
        if (preferred != null && !preferred.isEmpty()
            && (registry == null || registry.contains(preferred))) {
            return preferred;
        }
        if (fallback != null && !fallback.isEmpty()
            && (registry == null || registry.contains(fallback))) {
            return fallback;
        }
        return preferred == null || preferred.isEmpty() ? fallback : preferred;
    }
}
