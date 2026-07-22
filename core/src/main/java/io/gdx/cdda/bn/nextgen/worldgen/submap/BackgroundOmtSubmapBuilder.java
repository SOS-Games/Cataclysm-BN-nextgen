package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionResolver;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionGroundcoverSettings;

import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/** Simple 24×24 submaps for builtin-only OMT types (field, forest, river, road). */
public final class BackgroundOmtSubmapBuilder {

    private static final int SIZE = OmtStitchComposer.DEFAULT_OMT_SIZE;
    private static final String GRASS = "t_grass";
    private static final String SHALLOW_WATER = "t_water_sh";
    private static final String DEEP_WATER = "t_water_dp";
    private static final String REGION_SHRUB = "t_region_shrub";
    private static final String REGION_TREE = "t_region_tree";
    private static final String TRAIL_TERRAIN = "t_dirt";
    private static final String SAND = "t_sand";

    private BackgroundOmtSubmapBuilder() {}

    public static Optional<MapGrid> buildIfSupported(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final String omtId,
        final long previewSeed,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapTerrainRegistry oterRegistry,
        final RegionGroundcoverSettings groundcoverSettings
    ) {
        if (omtId == null || omtId.isEmpty()) {
            return Optional.empty();
        }
        final String normalized = normalize(omtId);
        final Random rng = new Random(previewSeed ^ normalized.hashCode());

        final RegionGroundcoverSettings groundcover = groundcoverSettings == null
            ? RegionGroundcoverSettings.defaults()
            : groundcoverSettings;

        if (isUndergroundTunnelOmt(normalized, connectionRegistry)) {
            return Optional.of(buildUndergroundTunnel(
                overmap,
                omtX,
                omtY,
                connectionRegistry,
                oterRegistry,
                previewSeed,
                groundcover
            ));
        }
        if (isForestTrailOmt(normalized, connectionRegistry)) {
            return Optional.of(buildForestTrail(
                overmap,
                omtX,
                omtY,
                connectionRegistry,
                oterRegistry,
                rng,
                previewSeed,
                groundcover
            ));
        }
        if (isRoadOmt(normalized, connectionRegistry)) {
            final String mapExtra = RoadMapExtras.roll(previewSeed ^ ((long) omtX << 16) ^ omtY, null);
            return Optional.of(BuiltinRoadMapgen.generate(
                overmap,
                omtX,
                omtY,
                omtId,
                connectionRegistry,
                oterRegistry,
                groundcover,
                previewSeed,
                true,
                mapExtra
            ));
        }
        if (isWaterOmt(normalized)) {
            return Optional.of(buildWater(normalized, rng));
        }
        if (isSwampOmt(normalized)) {
            return Optional.of(buildSwamp(rng, previewSeed, groundcover));
        }
        if (isForestOmt(normalized)) {
            return Optional.of(buildForest(rng, previewSeed, groundcover));
        }
        if (isFieldOmt(normalized)) {
            return Optional.of(buildField(rng, previewSeed, groundcover));
        }
        if (isBeachOmt(normalized)) {
            return Optional.of(buildBeach(rng));
        }
        if (isOpenLandOmt(normalized)) {
            return Optional.of(buildOpenLand(previewSeed, groundcover));
        }
        return Optional.empty();
    }

    private static MapGrid buildOpenLand(final long previewSeed, final RegionGroundcoverSettings groundcover) {
        return newMapWithGroundcover(previewSeed, groundcover);
    }

    private static MapGrid buildField(
        final Random rng,
        final long previewSeed,
        final RegionGroundcoverSettings groundcover
    ) {
        final MapGrid grid = newMapWithGroundcover(previewSeed, groundcover);
        scatterVegetation(grid, rng, 14, REGION_SHRUB, 6);
        return grid;
    }

    private static MapGrid buildForest(
        final Random rng,
        final long previewSeed,
        final RegionGroundcoverSettings groundcover
    ) {
        final MapGrid grid = newMapWithGroundcover(previewSeed, groundcover);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int roll = rng.nextInt(15);
                if (roll > 11) {
                    grid.setTerrain(x, y, REGION_TREE);
                } else if (roll > 9) {
                    grid.setTerrain(x, y, REGION_SHRUB);
                }
            }
        }
        return grid;
    }

    private static MapGrid buildSwamp(
        final Random rng,
        final long previewSeed,
        final RegionGroundcoverSettings groundcover
    ) {
        final MapGrid grid = newMapWithGroundcover(previewSeed, groundcover);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                final int roll = rng.nextInt(20);
                if (roll < 7) {
                    grid.setTerrain(x, y, SHALLOW_WATER);
                } else if (roll < 11) {
                    grid.setTerrain(x, y, REGION_SHRUB);
                } else if (roll == 19) {
                    grid.setTerrain(x, y, REGION_TREE);
                }
            }
        }
        return grid;
    }

    private static MapGrid buildWater(final String normalized, final Random rng) {
        final String fill = normalized.contains("center") || normalized.equals("lake") ? DEEP_WATER : SHALLOW_WATER;
        final MapGrid grid = new MapGrid(SIZE, SIZE, fill);
        if (normalized.equals("river") || normalized.startsWith("river_")) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if ((x + y + rng.nextInt(3)) % 5 == 0) {
                        grid.setTerrain(x, y, GRASS);
                    }
                }
            }
        }
        return grid;
    }

    private static MapGrid buildBeach(final Random rng) {
        final MapGrid grid = new MapGrid(SIZE, SIZE, SAND);
        for (int i = 0; i < SIZE * 2; i++) {
            grid.setTerrain(rng.nextInt(SIZE), rng.nextInt(SIZE), SHALLOW_WATER);
        }
        return grid;
    }

    private static MapGrid buildUndergroundTunnel(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapTerrainRegistry oterRegistry,
        final long previewSeed,
        final RegionGroundcoverSettings groundcover
    ) {
        final MapGrid grid = newMapWithGroundcover(previewSeed, groundcover);
        final boolean north = connectsUndergroundTunnel(overmap, omtX, omtY - 1, connectionRegistry, oterRegistry);
        final boolean east = connectsUndergroundTunnel(overmap, omtX + 1, omtY, connectionRegistry, oterRegistry);
        final boolean south = connectsUndergroundTunnel(overmap, omtX, omtY + 1, connectionRegistry, oterRegistry);
        final boolean west = connectsUndergroundTunnel(overmap, omtX - 1, omtY, connectionRegistry, oterRegistry);
        final boolean ns = north || south;
        final boolean ew = east || west;
        paintTrailBand(grid, ns, ew, north, east, south, west);
        if (!ns && !ew) {
            paintTrailBand(grid, true, true, true, true, true, true);
        }
        return grid;
    }

    private static boolean connectsUndergroundTunnel(
        final OvermapGrid overmap,
        final int x,
        final int y,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapTerrainRegistry oterRegistry
    ) {
        if (overmap == null || x < 0 || y < 0 || x >= overmap.width() || y >= overmap.height()) {
            return false;
        }
        final String neighbor = overmap.getOmtId(x, y);
        return isUndergroundTunnelOmt(normalize(neighbor), connectionRegistry);
    }

    private static MapGrid buildForestTrail(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapTerrainRegistry oterRegistry,
        final Random rng,
        final long previewSeed,
        final RegionGroundcoverSettings groundcover
    ) {
        final MapGrid grid = buildForest(rng, previewSeed, groundcover);
        final boolean north = connectsForestTrail(overmap, omtX, omtY - 1, connectionRegistry, oterRegistry);
        final boolean east = connectsForestTrail(overmap, omtX + 1, omtY, connectionRegistry, oterRegistry);
        final boolean south = connectsForestTrail(overmap, omtX, omtY + 1, connectionRegistry, oterRegistry);
        final boolean west = connectsForestTrail(overmap, omtX - 1, omtY, connectionRegistry, oterRegistry);
        final boolean ns = north || south;
        final boolean ew = east || west;
        paintTrailBand(grid, ns, ew, north, east, south, west);
        if (!ns && !ew) {
            paintTrailBand(grid, true, true, true, true, true, true);
        }
        return grid;
    }

    private static void paintTrailBand(
        final MapGrid grid,
        final boolean ns,
        final boolean ew,
        final boolean north,
        final boolean east,
        final boolean south,
        final boolean west
    ) {
        final int mid = SIZE / 2;
        final int half = 2;
        if (ns) {
            final int x0 = mid - half;
            final int x1 = mid + half - 1;
            for (int y = 0; y < SIZE; y++) {
                if (!north && y < mid) {
                    continue;
                }
                if (!south && y >= mid) {
                    continue;
                }
                for (int x = x0; x <= x1; x++) {
                    grid.setTerrain(x, y, TRAIL_TERRAIN);
                }
            }
        }
        if (ew) {
            final int y0 = mid - half;
            final int y1 = mid + half - 1;
            for (int x = 0; x < SIZE; x++) {
                if (!west && x < mid) {
                    continue;
                }
                if (!east && x >= mid) {
                    continue;
                }
                for (int y = y0; y <= y1; y++) {
                    grid.setTerrain(x, y, TRAIL_TERRAIN);
                }
            }
        }
    }

    private static boolean connectsForestTrail(
        final OvermapGrid overmap,
        final int x,
        final int y,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapTerrainRegistry oterRegistry
    ) {
        if (overmap == null || x < 0 || y < 0 || x >= overmap.width() || y >= overmap.height()) {
            return false;
        }
        final String neighbor = overmap.getOmtId(x, y);
        return isForestTrailOmt(normalize(neighbor), connectionRegistry);
    }

    private static boolean isRoadOmt(final String normalized, final OvermapConnectionRegistry connectionRegistry) {
        if (connectionRegistry != null) {
            final java.util.Optional<String> connectionId = OvermapConnectionResolver.connectionIdForOmt(
                normalized,
                connectionRegistry
            );
            if (connectionId.isPresent()) {
                return isRoadConnectionId(connectionId.get());
            }
        }
        return normalized.equals("road")
            || normalized.startsWith("road_")
            || normalized.startsWith("hiway_")
            || normalized.startsWith("test_road")
            || normalized.contains("railroad")
            || normalized.equals("bridge")
            || normalized.startsWith("bridge_");
    }

    private static boolean isForestTrailOmt(
        final String normalized,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        if (connectionRegistry != null) {
            final java.util.Optional<String> connectionId = OvermapConnectionResolver.connectionIdForOmt(
                normalized,
                connectionRegistry
            );
            if (connectionId.isPresent() && connectionId.get().contains("forest_trail")) {
                return true;
            }
        }
        return normalized.equals("forest_trail")
            || normalized.startsWith("forest_trail_")
            || normalized.equals("test_forest_trail");
    }

    private static boolean isUndergroundTunnelOmt(
        final String normalized,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        if (connectionRegistry != null) {
            final java.util.Optional<String> connectionId = OvermapConnectionResolver.connectionIdForOmt(
                normalized,
                connectionRegistry
            );
            if (connectionId.isPresent()) {
                final String id = connectionId.get();
                return id.contains("subway") || id.contains("sewer");
            }
        }
        return normalized.equals("subway")
            || normalized.startsWith("subway_")
            || normalized.equals("sewer")
            || normalized.startsWith("sewer_")
            || normalized.equals("test_subway")
            || normalized.equals("test_sewer");
    }

    private static boolean isRoadConnectionId(final String connectionId) {
        if (connectionId == null || connectionId.isEmpty()) {
            return false;
        }
        return !connectionId.contains("forest_trail")
            && !connectionId.contains("subway")
            && !connectionId.contains("sewer")
            && !connectionId.contains("railroad")
            && !connectionId.contains("rail");
    }

    private static boolean isWaterOmt(final String normalized) {
        return normalized.equals("lake")
            || normalized.equals("river")
            || normalized.startsWith("river_")
            || normalized.equals("water")
            || normalized.equals("test_river")
            || normalized.equals("test_lake")
            || normalized.equals("pond");
    }

    private static boolean isForestOmt(final String normalized) {
        return normalized.equals("forest")
            || normalized.startsWith("forest_")
            || normalized.equals("special_forest")
            || normalized.equals("test_forest")
            || normalized.equals("test_forest_thick");
    }

    private static boolean isSwampOmt(final String normalized) {
        return normalized.equals("forest_water")
            || normalized.equals("swamp")
            || normalized.equals("test_swamp");
    }

    private static boolean isFieldOmt(final String normalized) {
        return normalized.equals("field")
            || normalized.equals("test_field")
            || normalized.endsWith("_field");
    }

    private static boolean isBeachOmt(final String normalized) {
        return normalized.equals("beach") || normalized.equals("test_beach");
    }

    private static boolean isOpenLandOmt(final String normalized) {
        return normalized.equals("open_air")
            || normalized.equals("meadow")
            || normalized.equals("pasture");
    }

    private static MapGrid newMapWithGroundcover(final long previewSeed, final RegionGroundcoverSettings groundcover) {
        if (!groundcover.isWeighted()) {
            return new MapGrid(SIZE, SIZE, groundcover.getDefaultTerrainId());
        }
        final MapGrid grid = new MapGrid(SIZE, SIZE, groundcover.getDefaultTerrainId());
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                grid.setTerrain(x, y, groundcover.pickAt(previewSeed, x, y));
            }
        }
        return grid;
    }

    private static void scatterVegetation(
        final MapGrid grid,
        final Random rng,
        final int outOf,
        final String vegetationId,
        final int count
    ) {
        for (int i = 0; i < count; i++) {
            grid.setTerrain(rng.nextInt(SIZE), rng.nextInt(SIZE), vegetationId);
        }
    }

    private static String normalize(final String omtId) {
        return omtId.toLowerCase(Locale.ROOT);
    }
}
