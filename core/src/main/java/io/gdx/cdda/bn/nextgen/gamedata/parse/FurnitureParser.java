package io.gdx.cdda.bn.nextgen.gamedata.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses a `type: furniture` JSON object into registry entries (G3 v1 fields). */
public final class FurnitureParser {

    private FurnitureParser() {}

    public static void parseInto(
        final JsonValue jo,
        final String sourceMod,
        final FurnitureRegistry registry
    ) {
        if (jo == null || !jo.isObject()) {
            return;
        }
        if (!"furniture".equals(jo.getString("type", null))) {
            return;
        }

        final String id = jo.getString("id", null);
        if (id == null || id.isEmpty()) {
            return;
        }

        final String name = jo.getString("name", id);
        final String symbol = parseSymbol(jo.get("symbol"));
        final String color = parseColor(jo);
        final int moveCostMod = jo.getInt("move_cost_mod", 0);
        final int requiredStr = jo.getInt("required_str", -1);
        final List<String> flags = parseFlags(jo.get("flags"));
        final String looksLike = jo.getString("looks_like", null);

        registry.put(new FurnitureDefinition(
            id,
            name,
            symbol,
            color,
            moveCostMod,
            requiredStr,
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
