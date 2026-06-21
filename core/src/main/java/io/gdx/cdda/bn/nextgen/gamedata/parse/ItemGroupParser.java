package io.gdx.cdda.bn.nextgen.gamedata.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.ItemGroupDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.ItemGroupRegistry;

/** Parses a {@code type: item_group} JSON object into registry entries (G6). */
public final class ItemGroupParser {

    private ItemGroupParser() {}

    public static void parseInto(
        final JsonValue jo,
        final String sourceMod,
        final ItemGroupRegistry registry
    ) {
        if (jo == null || !jo.isObject()) {
            return;
        }
        if (!"item_group".equals(jo.getString("type", null))) {
            return;
        }

        final String id = jo.getString("id", null);
        if (id == null || id.isEmpty()) {
            return;
        }

        final String subtype = jo.getString("subtype", null);
        registry.put(new ItemGroupDefinition(id, subtype, sourceMod));
    }
}
