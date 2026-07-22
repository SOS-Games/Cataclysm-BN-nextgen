package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses {@code type: overmap_terrain} JSON objects (W1). */
public final class OvermapTerrainParser {

    private OvermapTerrainParser() {}

    public static List<OvermapTerrainDefinition> parseObject(
        final JsonValue root,
        final String sourceMod
    ) {
        return parseObject(root, sourceMod, null);
    }

    public static List<OvermapTerrainDefinition> parseObject(
        final JsonValue root,
        final String sourceMod,
        final List<String> resolvedFlags
    ) {
        if (root == null || !root.isObject()) {
            return Collections.emptyList();
        }
        if (!"overmap_terrain".equals(root.getString("type", null))) {
            return Collections.emptyList();
        }
        if (root.has("abstract") && !root.has("id")) {
            return Collections.emptyList();
        }

        final List<String> ids = parseIds(root.get("id"));
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        final String name = root.getString("name", null);
        final String symbol = parseSymbol(root.get("sym"));
        final String color = parseColor(root);
        final List<String> flags = resolvedFlags != null
            ? resolvedFlags
            : parseFlags(root.get("flags"));
        final List<MapgenRef> mapgenRefs = parseMapgenRefs(root.get("mapgen"));

        final List<OvermapTerrainDefinition> definitions = new ArrayList<>();
        for (final String id : ids) {
            if (id == null || id.isEmpty()) {
                continue;
            }
            definitions.add(new OvermapTerrainDefinition(
                id,
                name,
                symbol,
                color,
                flags,
                mapgenRefs,
                sourceMod
            ));
        }
        return definitions;
    }

    private static List<String> parseIds(final JsonValue idValue) {
        if (idValue == null || idValue.isNull()) {
            return Collections.emptyList();
        }
        if (idValue.isArray()) {
            final List<String> ids = new ArrayList<>();
            for (JsonValue child = idValue.child; child != null; child = child.next) {
                final String id = child.asString();
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                }
            }
            return ids;
        }
        final String id = idValue.asString();
        if (id == null || id.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(id);
    }

    private static String parseSymbol(final JsonValue symValue) {
        if (symValue == null || symValue.isNull()) {
            return "?";
        }
        final String sym = symValue.asString();
        if (sym == null || sym.isEmpty()) {
            return "?";
        }
        return sym.substring(0, 1);
    }

    private static String parseColor(final JsonValue root) {
        final String color = root.getString("color", null);
        if (color != null && !color.isEmpty()) {
            return color;
        }
        return null;
    }

    private static List<String> parseFlags(final JsonValue flagsValue) {
        if (flagsValue == null || !flagsValue.isArray()) {
            return Collections.emptyList();
        }
        final List<String> flags = new ArrayList<>();
        for (JsonValue child = flagsValue.child; child != null; child = child.next) {
            final String flag = child.asString();
            if (flag != null && !flag.isEmpty()) {
                flags.add(flag);
            }
        }
        return flags;
    }

    private static List<MapgenRef> parseMapgenRefs(final JsonValue mapgenValue) {
        if (mapgenValue == null || !mapgenValue.isArray()) {
            return Collections.emptyList();
        }
        final List<MapgenRef> refs = new ArrayList<>();
        for (JsonValue child = mapgenValue.child; child != null; child = child.next) {
            if (!child.isObject()) {
                continue;
            }
            final String method = child.getString("method", "");
            final String omTerrain = parseOmTerrainKey(child.get("om_terrain"));
            final int weight = child.getInt("weight", 100);
            refs.add(new MapgenRef(method, omTerrain, weight));
        }
        return refs;
    }

    private static String parseOmTerrainKey(final JsonValue omTerrainValue) {
        if (omTerrainValue == null || omTerrainValue.isNull()) {
            return "";
        }
        if (omTerrainValue.isArray()) {
            final JsonValue first = omTerrainValue.child;
            if (first == null) {
                return "";
            }
            if (first.isArray()) {
                final JsonValue nested = first.child;
                return nested == null ? "" : nullToEmpty(nested.asString());
            }
            return nullToEmpty(first.asString());
        }
        return nullToEmpty(omTerrainValue.asString());
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
