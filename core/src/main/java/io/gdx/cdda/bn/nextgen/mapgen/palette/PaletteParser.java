package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Parses {@code type: palette} JSON objects (P1). */
public final class PaletteParser {

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

        final Map<Integer, String> terrain = parseCharSection(root.get("terrain"));
        final Map<Integer, String> furniture = parseCharSection(root.get("furniture"));
        return Optional.of(new MapgenPalette(id, terrain, furniture));
    }

    public static Map<Integer, String> parseCharSection(final JsonValue section) {
        final Map<Integer, String> out = new HashMap<>();
        if (section == null || !section.isObject()) {
            return out;
        }
        for (JsonValue member = section.child; member != null; member = member.next) {
            final String key = member.name;
            if (key == null || key.isEmpty()) {
                continue;
            }
            final int codePoint = key.codePointAt(0);
            PaletteCharResolver.resolveId(member).ifPresent(id -> out.put(codePoint, id));
        }
        return out;
    }
}
