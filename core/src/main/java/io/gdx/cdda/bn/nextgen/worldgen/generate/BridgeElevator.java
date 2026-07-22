package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.List;
import java.util.Locale;

/**
 * Promotes road cells that sit on water / lake to bridge OMTs (R5 subset of BN elevate_bridges).
 */
public final class BridgeElevator {

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
                if (isBridgeId(id)) {
                    continue;
                }
                if (!touchesWater(grid, x, y, options, registry) && !isWaterId(id, options, registry)) {
                    continue;
                }
                // Only elevate when the road cell itself is water-typed (carved onto river) or
                // fully surrounded by water on the path — BN elevates IS_BRIDGE subtype paints.
                if (isWaterId(id, options, registry) || wasPaintedOverWater(grid, x, y, options, registry)) {
                    grid.setOmtId(x, y, bridgeId);
                    elevated++;
                }
            }
        }
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
                if (!RoadConnectionPolisher.isRoadFamily(id, connections) || isBridgeId(id)) {
                    continue;
                }
                final boolean n = isWaterAt(grid, x, y - 1, options, registry);
                final boolean s = isWaterAt(grid, x, y + 1, options, registry);
                final boolean e = isWaterAt(grid, x + 1, y, options, registry);
                final boolean w = isWaterAt(grid, x - 1, y, options, registry);
                if ((n && s) || (e && w)) {
                    grid.setOmtId(x, y, bridgeId);
                    elevated++;
                }
            }
        }
        return elevated;
    }

    private static boolean wasPaintedOverWater(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        // Heuristic: road cell with water on two opposite sides is a crossing.
        final boolean n = isWaterAt(grid, x, y - 1, options, registry);
        final boolean s = isWaterAt(grid, x, y + 1, options, registry);
        final boolean e = isWaterAt(grid, x + 1, y, options, registry);
        final boolean w = isWaterAt(grid, x - 1, y, options, registry);
        return (n && s) || (e && w);
    }

    private static boolean touchesWater(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        return isWaterAt(grid, x, y - 1, options, registry)
            || isWaterAt(grid, x, y + 1, options, registry)
            || isWaterAt(grid, x - 1, y, options, registry)
            || isWaterAt(grid, x + 1, y, options, registry);
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

    private static boolean isBridgeId(final String id) {
        if (id == null) {
            return false;
        }
        final String n = id.toLowerCase(Locale.ROOT);
        return n.equals("bridge") || n.startsWith("bridge_") || n.equals("test_bridge");
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

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
