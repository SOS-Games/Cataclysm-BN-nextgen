package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Carves in-city street grids inside urban blobs (W17b). */
public final class LocalRoadGenerator {

    private static final String LOCAL_ROAD_CONNECTION_ID = "local_road";
    private static final String TEST_LOCAL_ROAD_CONNECTION_ID = "test_local_road";
    private static final int GRID_SPACING = 3;

    private LocalRoadGenerator() {}

    public static int carveSites(
        final OvermapGrid grid,
        final List<UrbanSite> urbanSites,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || !options.isRoadsEnabled() || urbanSites == null || urbanSites.isEmpty()) {
            return 0;
        }
        final OvermapConnectionDefinition connection = resolveLocalRoad(connections, warnings);
        if (connection == null) {
            return 0;
        }
        final String roadId = resolveRoadTerrainId(connection, registry);
        if (registry != null && !registry.contains(roadId) && !hasDirectionalRoadSubtype(registry, connection)) {
            addWarning(warnings, "local road terrain '" + roadId + "' not in registry; skipping in-city roads");
            return 0;
        }

        int painted = 0;
        for (final UrbanSite site : urbanSites) {
            painted += carveSite(grid, site, connection, roadId, registry, options, rng);
        }
        return painted;
    }

    private static int carveSite(
        final OvermapGrid grid,
        final UrbanSite site,
        final OvermapConnectionDefinition connection,
        final String roadId,
        final OvermapTerrainRegistry registry,
        final OvermapGenerateOptions options,
        final Random rng
    ) {
        int painted = 0;
        painted += carveLine(
            grid,
            site,
            connection,
            roadId,
            registry,
            options,
            site.getCenterX() - site.getRadius(),
            site.getCenterY(),
            site.getCenterX() + site.getRadius(),
            site.getCenterY(),
            rng
        );
        painted += carveLine(
            grid,
            site,
            connection,
            roadId,
            registry,
            options,
            site.getCenterX(),
            site.getCenterY() - site.getRadius(),
            site.getCenterX(),
            site.getCenterY() + site.getRadius(),
            rng
        );

        for (int offset = -site.getRadius(); offset <= site.getRadius(); offset += GRID_SPACING) {
            if (offset == 0) {
                continue;
            }
            final int rowY = site.getCenterY() + offset;
            painted += carveLine(
                grid,
                site,
                connection,
                roadId,
                registry,
                options,
                site.getCenterX() - site.getRadius(),
                rowY,
                site.getCenterX() + site.getRadius(),
                rowY,
                rng
            );
            final int colX = site.getCenterX() + offset;
            painted += carveLine(
                grid,
                site,
                connection,
                roadId,
                registry,
                options,
                colX,
                site.getCenterY() - site.getRadius(),
                colX,
                site.getCenterY() + site.getRadius(),
                rng
            );
        }
        return painted;
    }

    private static int carveLine(
        final OvermapGrid grid,
        final UrbanSite site,
        final OvermapConnectionDefinition connection,
        final String roadId,
        final OvermapTerrainRegistry registry,
        final OvermapGenerateOptions options,
        final int startX,
        final int startY,
        final int endX,
        final int endY,
        final Random rng
    ) {
        final List<int[]> path = OrthogonalPathCarver.buildPath(startX, startY, endX, endY, rng);
        final Set<String> overwritable = buildOverwritableIds(options, registry, roadId, connection);
        return OrthogonalPathCarver.paintDirectionalPath(
            grid,
            path,
            (fromX, fromY, x, y, existing) -> {
                if (!site.contains(x, y) || !canOverwrite(existing, options)) {
                    return null;
                }
                final String picked = connection.pickTerrainForStep(fromX, fromY, x, y, existing, options);
                return OrthogonalPathCarver.resolveTerrainId(picked, roadId, registry);
            },
            overwritable
        );
    }

    private static Set<String> buildOverwritableIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final String roadId,
        final OvermapConnectionDefinition connection
    ) {
        final Set<String> ids = new HashSet<>(
            OrthogonalPathCarver.terrainOverwritableIds(options, registry, roadId)
        );
        if (connection != null) {
            for (final String subtype : connection.getSubtypeTerrains()) {
                if (subtype != null && !subtype.isEmpty()) {
                    ids.add(subtype);
                }
            }
        }
        ids.add("test_shop");
        ids.add("test_park");
        ids.add("test_urban_house");
        ids.add("test_finale");
        return ids;
    }

    private static boolean canOverwrite(final String existing, final OvermapGenerateOptions options) {
        if (existing == null || existing.isEmpty()) {
            return false;
        }
        if (options == null) {
            return true;
        }
        if (matchesProtectedId(existing, options.getRiverCenterId())
            || matchesProtectedId(existing, options.getRiverBankId())
            || matchesProtectedId(existing, options.getLakeId())) {
            return false;
        }
        return !matchesProtectedId(existing, "test_river")
            && !matchesProtectedId(existing, "test_lake")
            && !matchesProtectedId(existing, "test_swamp")
            && !matchesProtectedId(existing, "test_beach");
    }

    private static boolean matchesProtectedId(final String existing, final String candidate) {
        return candidate != null && !candidate.isEmpty() && candidate.equals(existing);
    }

    private static String resolveRoadTerrainId(
        final OvermapConnectionDefinition connection,
        final OvermapTerrainRegistry registry
    ) {
        return OrthogonalPathCarver.resolveTerrainId(connection.resolveTerrainId(), "test_road", registry);
    }

    private static boolean hasDirectionalRoadSubtype(
        final OvermapTerrainRegistry registry,
        final OvermapConnectionDefinition connection
    ) {
        if (registry == null) {
            return true;
        }
        for (final String terrainId : connection.getSubtypeTerrains()) {
            if (registry.contains(terrainId)) {
                return true;
            }
        }
        return false;
    }

    private static OvermapConnectionDefinition resolveLocalRoad(
        final OvermapConnectionRegistry connections,
        final List<String> warnings
    ) {
        if (connections == null) {
            addWarning(warnings, "connection registry missing; skipping in-city roads");
            return null;
        }
        final OvermapConnectionDefinition localRoad = connections.find(LOCAL_ROAD_CONNECTION_ID).orElse(null);
        if (localRoad != null) {
            return localRoad;
        }
        final OvermapConnectionDefinition testLocalRoad = connections.find(TEST_LOCAL_ROAD_CONNECTION_ID).orElse(null);
        if (testLocalRoad != null) {
            return testLocalRoad;
        }
        addWarning(warnings, "connection template '" + LOCAL_ROAD_CONNECTION_ID + "' not found; skipping in-city roads");
        return null;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
