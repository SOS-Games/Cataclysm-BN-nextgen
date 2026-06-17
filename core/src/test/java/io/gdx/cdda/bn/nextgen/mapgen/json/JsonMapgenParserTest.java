package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonReader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMapgenParserTest {

    @Test
    void flattensNestedOmTerrainGrid() {
        final String json = "{"
            + "\"type\":\"mapgen\","
            + "\"method\":\"json\","
            + "\"om_terrain\":["
            + "[\"tower_110\",\"tower_010\"],"
            + "[\"tower_100\",\"tower_000\"]"
            + "],"
            + "\"object\":{\"fill_ter\":\"t_floor\",\"rows\":[\"##\"]}"
            + "}";

        final JsonMapgenDefinition definition = JsonMapgenParser.parse(
            new JsonReader().parse(json),
            java.nio.file.Paths.get("nested.json"),
            0
        ).orElseThrow();

        assertEquals(4, definition.getOmTerrain().size());
        assertTrue(definition.getOmTerrain().contains("tower_000"));
        assertTrue(definition.getOmTerrainGrid().isPresent());
        assertEquals(2, definition.getOmTerrainGrid().get().width());
        assertEquals(2, definition.getOmTerrainGrid().get().height());
        assertEquals("tower_000", definition.getOmTerrainGrid().get().get(1, 1));
    }
}
