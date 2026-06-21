package io.gdx.cdda.bn.nextgen.gamedata.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupRegistry;

/** Parses a {@code type: monstergroup} JSON object into registry entries (G7). */
public final class MonsterGroupParser {

    private MonsterGroupParser() {}

    public static void parseInto(
        final JsonValue jo,
        final String sourceMod,
        final MonsterGroupRegistry registry
    ) {
        if (jo == null || !jo.isObject()) {
            return;
        }
        if (!"monstergroup".equals(jo.getString("type", null))) {
            return;
        }

        final String id = jo.getString("name", null);
        if (id == null || id.isEmpty()) {
            return;
        }

        final String defaultMonsterId = jo.getString("default", null);
        registry.put(new MonsterGroupDefinition(id, defaultMonsterId, sourceMod));
    }
}
