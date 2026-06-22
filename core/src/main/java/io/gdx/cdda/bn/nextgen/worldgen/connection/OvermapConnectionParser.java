package io.gdx.cdda.bn.nextgen.worldgen.connection;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses {@code type: overmap_connection} JSON (W5). */
public final class OvermapConnectionParser {

    private OvermapConnectionParser() {}

    public static OvermapConnectionDefinition parseObject(final JsonValue root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        if (!"overmap_connection".equals(root.getString("type", null))) {
            return null;
        }
        final String id = root.getString("id", null);
        if (id == null || id.isEmpty()) {
            return null;
        }
        final String defaultTerrain = root.getString("default_terrain", "road");
        final String bridgeTerrain = root.getString("bridge_terrain", null);
        final List<String> subtypeTerrains = parseSubtypeTerrains(root.get("subtypes"));
        return new OvermapConnectionDefinition(id, defaultTerrain, bridgeTerrain, subtypeTerrains);
    }

    private static List<String> parseSubtypeTerrains(final JsonValue subtypesValue) {
        if (subtypesValue == null || !subtypesValue.isArray()) {
            return Collections.emptyList();
        }
        final List<String> terrains = new ArrayList<>();
        for (JsonValue child = subtypesValue.child; child != null; child = child.next) {
            if (child == null || !child.isObject()) {
                continue;
            }
            final String terrain = child.getString("terrain", null);
            if (terrain != null && !terrain.isEmpty() && !terrains.contains(terrain)) {
                terrains.add(terrain);
            }
        }
        return terrains;
    }
}
