package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * BN {@code elevate_bridges} subset for the surface overmap (z=0).
 * Mid-span → {@code bridge_under}; ends → {@code bridgehead_ground_*};
 * one-tile spans flatten to directional road. Overpass / ramp at z+1 deferred
 * until {@link OvermapGrid} grows a z axis.
 */
public final class BridgeElevator {

    private static final int[][] DELTA = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

    private BridgeElevator() {}

    public static int elevate(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        if (grid == null || options == null) {
            return 0;
        }
        final String bridgeId = resolveBridgeId(options, connections, registry);
        if (bridgeId == null) {
            addWarning(warnings, "no bridge terrain available; skipping bridge elevation");
            return 0;
        }
        int elevated = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if (!RoadConnectionPolisher.isRoadFamily(id, connections)) {
                    continue;
                }
                if (isBridgeFamily(id)) {
                    continue;
                }
                if (isWaterId(id, options, registry) || wasPaintedOverWater(grid, x, y, options, registry)) {
                    grid.setOmtId(x, y, bridgeId);
                    elevated++;
                }
            }
        }
        elevated += elevateBridgeSpans(grid, options, connections, registry);
        return elevated;
    }

    /** Elevate road tiles whose neighbors include river/lake on opposite sides (crossing). */
    public static int elevateCrossings(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry
    ) {
        if (grid == null) {
            return 0;
        }
        final String bridgeId = resolveBridgeId(options, connections, registry);
        if (bridgeId == null) {
            return 0;
        }
        int elevated = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if ((!RoadConnectionPolisher.isRoadFamily(id, connections) && !isBridgeFamily(id))
                    || isElevatedBridgeId(id)) {
                    continue;
                }
                final boolean n = isWaterAt(grid, x, y - 1, options, registry);
                final boolean s = isWaterAt(grid, x, y + 1, options, registry);
                final boolean e = isWaterAt(grid, x + 1, y, options, registry);
                final boolean w = isWaterAt(grid, x - 1, y, options, registry);
                if ((n && s) || (e && w)) {
                    if (!isBridgeFamily(id)) {
                        grid.setOmtId(x, y, bridgeId);
                        elevated++;
                    }
                }
            }
        }
        elevated += elevateBridgeSpans(grid, options, connections, registry);
        return elevated;
    }

    /**
     * Rewrite contiguous bridge cells into under / bridgehead / flat road (BN elevate_bridges).
     */
    static int elevateBridgeSpans(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry
    ) {
        if (grid == null) {
            return 0;
        }
        final Set<Long> visited = new HashSet<>();
        int changed = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final long key = pack(x, y);
                if (visited.contains(key)) {
                    continue;
                }
                final String id = grid.getOmtId(x, y);
                if (!isRawBridgeId(id, registry)) {
                    continue;
                }
                final List<int[]> span = collectSpan(grid, x, y, visited, registry);
                if (span.isEmpty()) {
                    continue;
                }
                changed += applySpanElevation(grid, span, options, connections, registry);
            }
        }
        return changed;
    }

    private static List<int[]> collectSpan(
        final OvermapGrid grid,
        final int startX,
        final int startY,
        final Set<Long> visited,
        final OvermapTerrainRegistry registry
    ) {
        final List<int[]> cells = new ArrayList<>();
        final List<int[]> queue = new ArrayList<>();
        queue.add(new int[] { startX, startY });
        visited.add(pack(startX, startY));
        while (!queue.isEmpty()) {
            final int[] cur = queue.remove(queue.size() - 1);
            cells.add(cur);
            for (final int[] d : DELTA) {
                final int nx = cur[0] + d[0];
                final int ny = cur[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= grid.width() || ny >= grid.height()) {
                    continue;
                }
                final long key = pack(nx, ny);
                if (visited.contains(key)) {
                    continue;
                }
                if (!isRawBridgeId(grid.getOmtId(nx, ny), registry)) {
                    continue;
                }
                visited.add(key);
                queue.add(new int[] { nx, ny });
            }
        }
        orderSpan(cells);
        return cells;
    }

    private static void orderSpan(final List<int[]> cells) {
        if (cells.size() <= 1) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (final int[] c : cells) {
            minX = Math.min(minX, c[0]);
            maxX = Math.max(maxX, c[0]);
            minY = Math.min(minY, c[1]);
            maxY = Math.max(maxY, c[1]);
        }
        final boolean eastWest = (maxX - minX) >= (maxY - minY);
        cells.sort((a, b) -> eastWest
            ? Integer.compare(a[0], b[0])
            : Integer.compare(a[1], b[1]));
    }

    private static int applySpanElevation(
        final OvermapGrid grid,
        final List<int[]> span,
        final OvermapGenerateOptions options,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry
    ) {
        final String base = resolveBridgeBase(grid.getOmtId(span.get(0)[0], span.get(0)[1]), registry);
        final boolean eastWest = spanAxisEastWest(span);
        final String underId = resolveId(base + "_under", registry, base);
        final String flatId = eastWest
            ? resolveId("test_road_ew", registry, resolveId("road_ew", registry, base))
            : resolveId("test_road_ns", registry, resolveId("road_ns", registry, base));

        if (span.size() == 1) {
            final int[] p = span.get(0);
            final boolean ew = inferSingleTileEastWest(grid, p[0], p[1], options, registry);
            final String singleFlat = ew
                ? resolveId("test_road_ew", registry, resolveId("road_ew", registry, base))
                : resolveId("test_road_ns", registry, resolveId("road_ns", registry, base));
            grid.setOmtId(p[0], p[1], singleFlat);
            return 1;
        }

        int changed = 0;
        for (int i = 0; i < span.size(); i++) {
            final int[] p = span.get(i);
            final boolean head = i == 0 || i == span.size() - 1;
            if (head) {
                final int faceDir = headFaceDir(span, i, eastWest);
                final String headId = resolveBridgeheadId(base, faceDir, registry);
                grid.setOmtId(p[0], p[1], headId);
            } else {
                grid.setOmtId(p[0], p[1], underId);
            }
            changed++;
        }
        return changed;
    }

    private static int headFaceDir(final List<int[]> span, final int index, final boolean eastWest) {
        // Ramp faces away from the span (toward land), matching BN bridgehead orientation.
        if (index == 0) {
            return eastWest ? 3 : 0; // west or north
        }
        return eastWest ? 1 : 2; // east or south
    }

    private static boolean spanAxisEastWest(final List<int[]> span) {
        if (span.size() < 2) {
            return false;
        }
        return span.get(0)[1] == span.get(span.size() - 1)[1];
    }

    /** One-tile bridge: prefer the axis that has water on both sides (crossing). */
    private static boolean inferSingleTileEastWest(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final boolean n = isWaterAt(grid, x, y - 1, options, registry);
        final boolean s = isWaterAt(grid, x, y + 1, options, registry);
        final boolean e = isWaterAt(grid, x + 1, y, options, registry);
        final boolean w = isWaterAt(grid, x - 1, y, options, registry);
        if (e && w) {
            return true;
        }
        if (n && s) {
            return false;
        }
        return e || w;
    }

    private static String resolveBridgeBase(final String id, final OvermapTerrainRegistry registry) {
        if (id != null) {
            if (id.startsWith("test_bridge")) {
                return "test_bridge";
            }
            if (id.equals("bridge") || id.startsWith("bridge_")) {
                return "bridge";
            }
        }
        if (registry != null && registry.contains("test_bridge")) {
            return "test_bridge";
        }
        return "bridge";
    }

    private static String resolveBridgeheadId(
        final String base,
        final int faceDir,
        final OvermapTerrainRegistry registry
    ) {
        final String[] suffixes = { "_north", "_east", "_south", "_west" };
        final String withDir = base + "head_ground" + suffixes[faceDir];
        if (registry == null || registry.contains(withDir)) {
            return withDir;
        }
        final String bare = base + "head_ground";
        if (registry.contains(bare)) {
            return bare;
        }
        return resolveId(base + "_under", registry, base);
    }

    private static String resolveId(
        final String preferred,
        final OvermapTerrainRegistry registry,
        final String fallback
    ) {
        if (registry == null || registry.contains(preferred)) {
            return preferred;
        }
        return fallback;
    }

    private static boolean wasPaintedOverWater(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final boolean n = isWaterAt(grid, x, y - 1, options, registry);
        final boolean s = isWaterAt(grid, x, y + 1, options, registry);
        final boolean e = isWaterAt(grid, x + 1, y, options, registry);
        final boolean w = isWaterAt(grid, x - 1, y, options, registry);
        return (n && s) || (e && w);
    }

    private static boolean isWaterAt(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return isWaterId(grid.getOmtId(x, y), options, registry);
    }

    private static boolean isWaterId(
        final String id,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (id == null) {
            return false;
        }
        if (options != null) {
            if (id.equals(options.getRiverCenterId()) || id.equals(options.getRiverBankId())
                || id.equals(options.getLakeId())) {
                return true;
            }
        }
        final String n = id.toLowerCase(Locale.ROOT);
        if (n.contains("river") || n.contains("lake") || n.equals("test_river") || n.startsWith("test_lake")) {
            return true;
        }
        if (registry != null) {
            final OvermapTerrainDefinition def = registry.find(id).orElse(null);
            if (def != null) {
                for (final String flag : def.getFlags()) {
                    if ("RIVER".equals(flag) || "LAKE".equals(flag)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Raw bridge paint before elevate_bridges rewrite (IS_BRIDGE / bridge_terrain id). */
    private static boolean isRawBridgeId(final String id, final OvermapTerrainRegistry registry) {
        if (id == null) {
            return false;
        }
        if (isElevatedBridgeId(id)) {
            return false;
        }
        final String n = id.toLowerCase(Locale.ROOT);
        if (n.equals("bridge") || n.equals("test_bridge")) {
            return true;
        }
        if (registry != null) {
            final OvermapTerrainDefinition def = registry.find(id).orElse(null);
            if (def != null) {
                for (final String flag : def.getFlags()) {
                    if ("IS_BRIDGE".equals(flag)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isElevatedBridgeId(final String id) {
        if (id == null) {
            return false;
        }
        final String n = id.toLowerCase(Locale.ROOT);
        return n.contains("bridge_under")
            || n.contains("bridgehead")
            || n.contains("bridge_road")
            || n.contains("bridge_center");
    }

    static boolean isBridgeFamily(final String id) {
        if (id == null) {
            return false;
        }
        final String n = id.toLowerCase(Locale.ROOT);
        return n.equals("bridge")
            || n.startsWith("bridge_")
            || n.startsWith("bridgehead")
            || n.equals("test_bridge")
            || n.startsWith("test_bridge");
    }

    private static String resolveBridgeId(
        final OvermapGenerateOptions options,
        final OvermapConnectionRegistry connections,
        final OvermapTerrainRegistry registry
    ) {
        if (connections != null && options != null) {
            final OvermapConnectionDefinition def = connections.find(options.getConnectionId()).orElse(null);
            if (def != null && def.getBridgeTerrain() != null && !def.getBridgeTerrain().isEmpty()) {
                if (registry == null || registry.contains(def.getBridgeTerrain())) {
                    return def.getBridgeTerrain();
                }
            }
            for (final String id : new String[] { "local_road", "test_local_road" }) {
                final OvermapConnectionDefinition c = connections.find(id).orElse(null);
                if (c != null && c.getBridgeTerrain() != null && !c.getBridgeTerrain().isEmpty()) {
                    if (registry == null || registry.contains(c.getBridgeTerrain())) {
                        return c.getBridgeTerrain();
                    }
                }
            }
        }
        if (registry != null) {
            if (registry.contains("bridge")) {
                return "bridge";
            }
            if (registry.contains("test_bridge")) {
                return "test_bridge";
            }
        }
        return "bridge";
    }

    private static long pack(final int x, final int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
