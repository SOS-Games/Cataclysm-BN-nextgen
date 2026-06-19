package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Parses {@code type: palette} JSON objects (P1, P10). */
public final class PaletteParser {

    private static final JsonReader JSON_READER = new JsonReader();

    private PaletteParser() {}

    public static Optional<MapgenPalette> parse(final JsonValue root) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        if (!"palette".equals(root.getString("type", ""))) {
            return Optional.empty();
        }
        final String id = root.getString("id", null);
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }

        final List<String> parentIds = readParentIds(root);
        final Map<Integer, JsonValue> terrain = parseCharSectionNodes(root.get("terrain"));
        final Map<Integer, JsonValue> furniture = parseCharSectionNodes(root.get("furniture"));
        final Map<Integer, Integer> translate = parseTranslateSection(root.get("translate"));
        return Optional.of(new MapgenPalette(id, parentIds, terrain, furniture, translate));
    }

    public static Map<Integer, String> parseCharSection(final JsonValue section) {
        final Map<Integer, String> out = new HashMap<>();
        for (final Map.Entry<Integer, JsonValue> entry : parseCharSectionNodes(section).entrySet()) {
            PaletteCharResolver.resolveId(entry.getValue()).ifPresent(id -> out.put(entry.getKey(), id));
        }
        return out;
    }

    public static Map<Integer, JsonValue> parseCharSectionNodes(final JsonValue section) {
        final Map<Integer, JsonValue> out = new HashMap<>();
        if (section == null || !section.isObject()) {
            return out;
        }
        for (JsonValue member = section.child; member != null; member = member.next) {
            final String key = member.name;
            if (key == null || key.isEmpty()) {
                continue;
            }
            out.put(key.codePointAt(0), copyValue(member));
        }
        return out;
    }

    private static List<String> readParentIds(final JsonValue root) {
        final JsonValue parents = root.get("palettes");
        if (parents == null || !parents.isArray()) {
            return List.of();
        }
        final List<String> parentIds = new ArrayList<>();
        for (JsonValue child = parents.child; child != null; child = child.next) {
            if (!child.isString()) {
                continue;
            }
            final String parentId = child.asString();
            if (parentId != null && !parentId.isEmpty()) {
                parentIds.add(parentId);
            }
        }
        return parentIds;
    }

    private static Map<Integer, Integer> parseTranslateSection(final JsonValue section) {
        final Map<Integer, Integer> out = new HashMap<>();
        if (section == null || !section.isObject()) {
            return out;
        }
        for (JsonValue member = section.child; member != null; member = member.next) {
            final String key = member.name;
            if (key == null || key.isEmpty()) {
                continue;
            }
            final String target = member.isString() ? member.asString() : null;
            if (target == null || target.isEmpty()) {
                continue;
            }
            out.put(key.codePointAt(0), target.codePointAt(0));
        }
        return out;
    }

    static JsonValue copyValue(final JsonValue value) {
        if (value == null) {
            return null;
        }
        if (value.isString()) {
            return new JsonValue(value.asString());
        }
        if (value.isBoolean()) {
            return new JsonValue(value.asBoolean());
        }
        if (value.isDouble() || value.isLong() || value.isNumber()) {
            return new JsonValue(value.asDouble());
        }
        if (value.isArray()) {
            return copyArray(value);
        }
        if (value.isObject()) {
            return copyObject(value);
        }
        return new JsonValue(value.asString());
    }

    private static JsonValue copyArray(final JsonValue array) {
        final JsonValue copy = new JsonValue(JsonValue.ValueType.array);
        for (JsonValue child = array.child; child != null; child = child.next) {
            copy.addChild(copyValue(child));
        }
        return copy;
    }

    private static JsonValue copyObject(final JsonValue object) {
        final JsonValue copy = new JsonValue(JsonValue.ValueType.object);
        for (JsonValue child = object.child; child != null; child = child.next) {
            copy.addChild(child.name, copyValue(child));
        }
        return copy;
    }
}
