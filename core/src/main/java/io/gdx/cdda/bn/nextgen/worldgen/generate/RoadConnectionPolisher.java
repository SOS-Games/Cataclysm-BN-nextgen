package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OmLines;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionResolver;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;
import java.util.Locale;

/** Rewrites road OMT cells to LINEAR directional ids from NESW neighbors (R1). */
public final class RoadConnectionPolisher {

    private RoadConnectionPolisher() {}

    public static int polish(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        if (grid == null || options == null) {
            return 0;
        }
        final String preferredBase = resolvePreferredBase(options, connections, registry);
        final String[][] snapshot = snapshot(grid);
        int polished = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String omtId = snapshot[y][x];
                if (!isRoadFamily(omtId, connections)) {
                    continue;
                }
                if (isBridge(omtId)) {
                    continue;
                }
                final String base = OmLines.stripToBase(omtId, preferredBase, "road", "test_road");
                if (base == null) {
                    continue;
                }
                final boolean north = isRoadNeighbor(snapshot, grid, x, y - 1, connections);
                final boolean east = isRoadNeighbor(snapshot, grid, x + 1, y, connections);
                final boolean south = isRoadNeighbor(snapshot, grid, x, y + 1, connections);
                final boolean west = isRoadNeighbor(snapshot, grid, x - 1, y, connections);
                final int line = OmLines.fromCardinals(north, east, south, west);
                final String polishedId = pickLinearId(base, line, omtId, registry);
                if (polishedId != null && !polishedId.equals(omtId)) {
                    grid.setOmtId(x, y, polishedId);
                    polished++;
                }
            }
        }
        if (polished == 0 && preferredBase == null) {
            addWarning(warnings, "road LINEAR polish found no preferred base terrain");
        }
        return polished;
    }

    static String pickLinearId(
        final String base,
        final int line,
        final String existingId,
        final OvermapTerrainRegistry registry
    ) {
        if (existingId != null && existingId.contains("manhole")) {
            return existingId;
        }
        final String candidate = OmLines.idFor(base, line);
        if (registry == null || registry.contains(candidate)) {
            return candidate;
        }
        if (line == OmLines.BITS && registry.contains(base + "_nesw_manhole")) {
            return base + "_nesw_manhole";
        }
        // Fall back through coarser shapes when peers are missing from fixtures.
        if (OmLines.hasSegment(line, OmLines.NORTH) && OmLines.hasSegment(line, OmLines.SOUTH)
            && registry.contains(base + "_ns")) {
            if (!OmLines.hasSegment(line, OmLines.EAST) && !OmLines.hasSegment(line, OmLines.WEST)) {
                return base + "_ns";
            }
        }
        if (OmLines.hasSegment(line, OmLines.EAST) && OmLines.hasSegment(line, OmLines.WEST)
            && registry.contains(base + "_ew")) {
            if (!OmLines.hasSegment(line, OmLines.NORTH) && !OmLines.hasSegment(line, OmLines.SOUTH)) {
                return base + "_ew";
            }
        }
        if (registry.contains(base)) {
            return base;
        }
        return existingId;
    }

    public static boolean isRoadFamily(final String omtId, final OvermapConnectionRegistry connections) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        final String normalized = omtId.toLowerCase(Locale.ROOT);
        if (connections != null) {
            final java.util.Optional<String> connectionId = OvermapConnectionResolver.connectionIdForOmt(
                normalized,
                connections
            );
            if (connectionId.isPresent()) {
                final String id = connectionId.get();
                return id.contains("road") && !id.contains("rail");
            }
        }
        return normalized.equals("road")
            || normalized.startsWith("road_")
            || normalized.equals("test_road")
            || normalized.startsWith("test_road_")
            || normalized.startsWith("hiway_");
    }

    private static boolean isBridge(final String omtId) {
        if (omtId == null) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        return n.equals("bridge") || n.startsWith("bridge_") || n.equals("test_bridge");
    }

    private static boolean isRoadNeighbor(
        final String[][] snapshot,
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapConnectionRegistry connections
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        final String id = snapshot[y][x];
        return isRoadFamily(id, connections) || isBridge(id);
    }

    private static String resolvePreferredBase(
        final OvermapGenerateOptions options,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry
    ) {
        if (connections != null) {
            for (final String id : new String[] { "local_road", "test_local_road", options.getConnectionId() }) {
                if (id == null || id.isEmpty()) {
                    continue;
                }
                final OvermapConnectionDefinition def = connections.find(id).orElse(null);
                if (def != null) {
                    final String terrain = def.resolveTerrainId();
                    if (registry == null || registry.contains(terrain) || registry.contains(terrain + "_ns")) {
                        return terrain;
                    }
                }
            }
        }
        if (registry != null) {
            if (registry.contains("road") || registry.contains("road_ns")) {
                return "road";
            }
            if (registry.contains("test_road") || registry.contains("test_road_ns")) {
                return "test_road";
            }
        }
        return "road";
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

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
