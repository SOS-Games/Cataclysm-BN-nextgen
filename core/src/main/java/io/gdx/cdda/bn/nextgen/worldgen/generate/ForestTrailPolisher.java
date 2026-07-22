package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OmLines;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

/**
 * Rewrites {@code forest_trail} cells to LINEAR directional ids from NESW neighbors,
 * matching BN {@code build_connection} line-bit merge so trails stay distinct from forest/field.
 */
public final class ForestTrailPolisher {

    private ForestTrailPolisher() {}

    public static int polish(
        final OvermapGrid grid,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry
    ) {
        if (grid == null) {
            return 0;
        }
        final String preferredBase = resolveTrailBase(grid, registry);
        if (preferredBase == null) {
            return 0;
        }
        final String[][] snapshot = snapshot(grid);
        int polished = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String omtId = snapshot[y][x];
                if (!isTrailFamily(omtId)) {
                    continue;
                }
                final boolean north = isTrailNeighbor(snapshot, grid, x, y - 1);
                final boolean east = isTrailNeighbor(snapshot, grid, x + 1, y);
                final boolean south = isTrailNeighbor(snapshot, grid, x, y + 1);
                final boolean west = isTrailNeighbor(snapshot, grid, x - 1, y);
                final int line = OmLines.fromCardinals(north, east, south, west);
                final String candidate = OmLines.idFor(preferredBase, line);
                final String writeId = pickWritableId(
                    preferredBase, candidate, north, east, south, west, registry, omtId
                );
                if (writeId != null && !writeId.equals(omtId)) {
                    grid.setOmtId(x, y, writeId);
                    polished++;
                }
            }
        }
        return polished;
    }

    private static String pickWritableId(
        final String base,
        final String candidate,
        final boolean north,
        final boolean east,
        final boolean south,
        final boolean west,
        final OvermapTerrainRegistry registry,
        final String existing
    ) {
        if (candidate != null && (registry == null || registry.contains(candidate) || isLinearBase(base, registry))) {
            return candidate;
        }
        if ((north || south) && !east && !west) {
            final String ns = base + "_ns";
            if (registry == null || registry.contains(ns)) {
                return ns;
            }
        }
        if ((east || west) && !north && !south) {
            final String ew = base + "_ew";
            if (registry == null || registry.contains(ew)) {
                return ew;
            }
        }
        return existing;
    }

    private static boolean isLinearBase(final String base, final OvermapTerrainRegistry registry) {
        if (base == null || base.isEmpty() || registry == null) {
            return false;
        }
        return registry.find(base)
            .map(def -> {
                for (final String flag : def.getFlags()) {
                    if ("LINEAR".equals(flag)) {
                        return true;
                    }
                }
                return false;
            })
            .orElse(false);
    }

    private static String resolveTrailBase(final OvermapGrid grid, final OvermapTerrainRegistry registry) {
        if (registry != null && registry.contains("forest_trail")) {
            return "forest_trail";
        }
        if (registry != null && registry.contains("test_forest_trail")) {
            return "test_forest_trail";
        }
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if (isTrailFamily(id)) {
                    return id.startsWith("test_forest_trail") ? "test_forest_trail" : "forest_trail";
                }
            }
        }
        return null;
    }

    static boolean isTrailFamily(final String omtId) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        return omtId.startsWith("forest_trail") || omtId.startsWith("test_forest_trail");
    }

    private static boolean isTrailNeighbor(
        final String[][] snapshot,
        final OvermapGrid grid,
        final int x,
        final int y
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return isTrailFamily(snapshot[y][x]);
    }

    private static String[][] snapshot(final OvermapGrid grid) {
        final String[][] out = new String[grid.height()][grid.width()];
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                out[y][x] = grid.getOmtId(x, y);
            }
        }
        return out;
    }
}
