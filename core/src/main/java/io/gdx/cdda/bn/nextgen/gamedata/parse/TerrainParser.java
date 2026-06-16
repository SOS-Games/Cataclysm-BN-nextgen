package io.gdx.cdda.bn.nextgen.gamedata.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses a `type: terrain` JSON object into registry entries (G2 v1 fields). */
public final class TerrainParser {

    private TerrainParser() {}

    public static void parseInto(
        final JsonValue jo,
        final String sourceMod,
        final TerrainRegistry registry
    ) {
        if (jo == null || !jo.isObject()) {
            return;
        }
        if (!"terrain".equals(jo.getString("type", null))) {
            return;
        }

        final String id = jo.getString("id", null);
        if (id == null || id.isEmpty()) {
            return;
        }

        final String name = jo.getString("name", id);
        final String description = jo.getString("description", null);
        final String symbol = parseSymbol(jo.get("symbol"));
        final String color = parseColor(jo);
        final int moveCost = jo.getInt("move_cost", 0);
        final List<String> flags = parseFlags(jo.get("flags"));
        final String looksLike = jo.getString("looks_like", null);

        registry.put(new TerrainDefinition(
            id,
            name,
            description,
            symbol,
            color,
            moveCost,
            flags,
            looksLike,
            sourceMod
        ));
    }

    private static String parseSymbol(final JsonValue symbolValue) {
        if (symbolValue == null || symbolValue.isNull()) {
            return "?";
        }
        if (symbolValue.isArray()) {
            final JsonValue first = symbolValue.child;
            return first == null ? "?" : parseSymbol(first);
        }
        final String symbol = symbolValue.asString();
        if (symbol == null || symbol.isEmpty()) {
            return "?";
        }
        if ("LINE_XOXO".equals(symbol) || "LINE_OXOX".equals(symbol)) {
            return symbol;
        }
        return symbol.substring(0, 1);
    }

    private static String parseColor(final JsonValue jo) {
        final String color = jo.getString("color", null);
        if (color != null && !color.isEmpty()) {
            return color;
        }
        final String bgcolor = jo.getString("bgcolor", null);
        return bgcolor == null || bgcolor.isEmpty() ? null : bgcolor;
    }

    private static List<String> parseFlags(final JsonValue flagsValue) {
        if (flagsValue == null || !flagsValue.isArray()) {
            return Collections.emptyList();
        }
        final List<String> out = new ArrayList<>();
        for (JsonValue entry = flagsValue.child; entry != null; entry = entry.next) {
            final String flag = entry.asString();
            if (flag != null && !flag.isEmpty()) {
                out.add(flag);
            }
        }
        return out;
    }
}
