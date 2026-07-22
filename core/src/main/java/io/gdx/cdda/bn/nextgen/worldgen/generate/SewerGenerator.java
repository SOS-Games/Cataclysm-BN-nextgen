package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** In-city sewer grid carving via {@code sewer_tunnel} (W17f lite). */
public final class SewerGenerator {

    private static final String SEWER_CONNECTION_ID = "sewer_tunnel";
    private static final String TEST_SEWER_CONNECTION_ID = "test_sewer_tunnel";
    private static final int GRID_SPACING = 4;

    private SewerGenerator() {}

    public static int carveSites(
        final OvermapGrid grid,
        final List<UrbanSite> urbanSites,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || urbanSites == null || urbanSites.isEmpty()) {
            return 0;
        }
        final String connectionId = resolveConnectionId(connections);
        final OvermapConnectionDefinition connection = ConnectionPathGenerator.resolveConnection(
            connections,
            connectionId,
            warnings
        );
        if (connection == null) {
            return 0;
        }
        // BN places sewers at z-1 under the city. Until overmap Z exists, painting
        // "sewer" on the surface punches lots beside roads and breaks sidewalks.
        addWarning(warnings, "sewer carve skipped: underground z-layer not implemented (BN uses z-1)");
        return 0;
    }

    /** Surface carve kept for when overmap Z lands; currently unused. */
    @SuppressWarnings("unused")
    private static int carveSite(
        final OvermapGrid grid,
        final UrbanSite site,
        final OvermapConnectionDefinition connection,
        final String sewerId,
        final OvermapTerrainRegistry registry,
        final OvermapGenerateOptions options,
        final Random rng
    ) {
        int painted = 0;
        painted += carveLine(
            grid,
            site,
            connection,
            sewerId,
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
            sewerId,
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
                sewerId,
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
                sewerId,
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
        final String sewerId,
        final OvermapTerrainRegistry registry,
        final OvermapGenerateOptions options,
        final int startX,
        final int startY,
        final int endX,
        final int endY,
        final Random rng
    ) {
        final List<int[]> path = OrthogonalPathCarver.buildPath(startX, startY, endX, endY, rng);
        final Set<String> overwritable = buildOverwritableIds(options, registry, sewerId, connection);
        return OrthogonalPathCarver.paintDirectionalPath(
            grid,
            path,
            (fromX, fromY, x, y, existing) -> {
                if (!site.contains(x, y) || !canOverwrite(existing, options)) {
                    return null;
                }
                final String picked = connection.pickTerrainForStep(fromX, fromY, x, y, existing, options);
                return OrthogonalPathCarver.resolveTerrainId(picked, sewerId, registry);
            },
            overwritable
        );
    }

    private static Set<String> buildOverwritableIds(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final String sewerId,
        final OvermapConnectionDefinition connection
    ) {
        final Set<String> ids = new HashSet<>(
            OrthogonalPathCarver.terrainOverwritableIds(options, registry, sewerId)
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
        ids.add("test_field");
        ids.add("open_air");
        // Do not include roads — surface sewers must not overwrite the street grid.
        return ids;
    }

    private static boolean canOverwrite(final String existing, final OvermapGenerateOptions options) {
        if (existing == null || existing.isEmpty()) {
            return false;
        }
        // BN sewers live at z-1; never punch surface roads / bridges (breaks sidewalks + routing).
        if (UrbanTerrainClearables.isRoadFamily(existing) || existing.contains("bridge")) {
            return false;
        }
        if (existing.equals(options.getFieldId()) || existing.equals(options.getForestId())) {
            return true;
        }
        final String n = existing.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("house")
            || n.contains("shop")
            || n.contains("bungalow")
            || n.contains("building")
            || n.contains("station")
            || n.contains("cemetery")
            || n.contains("garden")
            || n.contains("park")
            || n.contains("lot")) {
            return false;
        }
        return existing.startsWith("test_")
            || existing.contains("sewer")
            || n.equals("field")
            || n.equals("open_air")
            || n.startsWith("forest");
    }

    private static boolean hasSubtype(
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

    private static String resolveConnectionId(final OvermapConnectionRegistry connections) {
        if (connections != null && connections.contains(SEWER_CONNECTION_ID)) {
            return SEWER_CONNECTION_ID;
        }
        if (connections != null && connections.contains(TEST_SEWER_CONNECTION_ID)) {
            return TEST_SEWER_CONNECTION_ID;
        }
        return SEWER_CONNECTION_ID;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null && message != null && !message.isEmpty()) {
            warnings.add(message);
        }
    }
}
