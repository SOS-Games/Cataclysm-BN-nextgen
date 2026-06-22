package io.gdx.cdda.bn.nextgen.worldgen.connection;

import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.CardinalDirection;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Maps neighbor OMT ids to {@link OvermapConnectionDefinition} ids for nested mapgen (W13c). */
public final class OvermapConnectionResolver {

    private OvermapConnectionResolver() {}

    public static Map<String, String> connectionsByDirection(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final OvermapConnectionRegistry registry
    ) {
        if (overmap == null || registry == null || registry.size() == 0) {
            return Collections.emptyMap();
        }
        final Map<String, String> connections = new LinkedHashMap<>();
        for (final CardinalDirection direction : CardinalDirection.values()) {
            final int x = omtX + direction.getDx();
            final int y = omtY + direction.getDy();
            if (x < 0 || y < 0 || x >= overmap.width() || y >= overmap.height()) {
                continue;
            }
            final String omtId = overmap.getOmtId(x, y);
            connectionIdForOmt(omtId, registry).ifPresent(id ->
                connections.put(direction.name().toLowerCase(Locale.ROOT), id)
            );
        }
        return connections;
    }

    public static Optional<String> connectionIdForOmt(
        final String omtId,
        final OvermapConnectionRegistry registry
    ) {
        if (omtId == null || omtId.isEmpty() || registry == null || registry.size() == 0) {
            return Optional.empty();
        }
        final String normalized = normalizeOmtId(omtId);
        for (final OvermapConnectionDefinition definition : registry.all()) {
            if (matchesTerrain(normalized, definition.getDefaultTerrain())) {
                return Optional.of(definition.getId());
            }
            if (definition.getBridgeTerrain() != null
                && matchesTerrain(normalized, definition.getBridgeTerrain())) {
                return Optional.of(definition.getId());
            }
            for (final String subtype : definition.getSubtypeTerrains()) {
                if (matchesTerrain(normalized, subtype)) {
                    return Optional.of(definition.getId());
                }
            }
        }
        return Optional.empty();
    }

    private static String normalizeOmtId(final String omtId) {
        return OvermapTerrainResolver.stripRotation(omtId).toLowerCase(Locale.ROOT);
    }

    private static boolean matchesTerrain(final String normalizedOmtId, final String terrainId) {
        if (terrainId == null || terrainId.isEmpty()) {
            return false;
        }
        final String normalizedTerrain = terrainId.toLowerCase(Locale.ROOT);
        return normalizedOmtId.equals(normalizedTerrain)
            || normalizedOmtId.startsWith(normalizedTerrain + "_");
    }
}
