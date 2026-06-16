package io.gdx.cdda.bn.nextgen.gamedata;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.TerrainParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainParserTest {

    @Test
    void parsesV1FieldsForTerrainObject() {
        final JsonValue jo = new JsonReader().parse(
            "{"
                + "\"type\":\"terrain\","
                + "\"id\":\"t_dirt\","
                + "\"name\":\"dirt\","
                + "\"description\":\"It's dirt.\","
                + "\"symbol\":\".\","
                + "\"color\":\"brown\","
                + "\"move_cost\":2,"
                + "\"flags\":[\"TRANSPARENT\",\"FLAT\"],"
                + "\"looks_like\":\"t_mud\""
                + "}"
        );
        final TerrainRegistry registry = new TerrainRegistry();

        TerrainParser.parseInto(jo, "core", registry);

        final TerrainDefinition terrain = registry.find("t_dirt").orElseThrow();
        assertEquals("dirt", terrain.getName());
        assertEquals(".", terrain.getSymbol());
        assertEquals("brown", terrain.getColor());
        assertEquals(2, terrain.getMoveCost());
        assertEquals("t_mud", terrain.getLooksLike());
        assertEquals("core", terrain.getSourceMod());
        assertEquals(2, terrain.getFlags().size());
    }

    @Test
    void secondDefinitionOverridesFirstById() {
        final JsonValue first = new JsonReader().parse(
            "{"
                + "\"type\":\"terrain\","
                + "\"id\":\"t_fixture\","
                + "\"name\":\"first\","
                + "\"symbol\":\".\","
                + "\"color\":\"brown\","
                + "\"move_cost\":2"
                + "}"
        );
        final JsonValue second = new JsonReader().parse(
            "{"
                + "\"type\":\"terrain\","
                + "\"id\":\"t_fixture\","
                + "\"name\":\"second\","
                + "\"symbol\":\"#\","
                + "\"color\":\"light_gray\","
                + "\"move_cost\":0"
                + "}"
        );
        final TerrainRegistry registry = new TerrainRegistry();

        TerrainParser.parseInto(first, "core", registry);
        TerrainParser.parseInto(second, "core", registry);

        final TerrainDefinition terrain = registry.find("t_fixture").orElseThrow();
        assertEquals("second", terrain.getName());
        assertEquals("#", terrain.getSymbol());
        assertEquals(0, terrain.getMoveCost());
    }

    @Test
    void keepsLineDrawingSymbolsLiteral() {
        final JsonValue jo = new JsonReader().parse(
            "{"
                + "\"type\":\"terrain\","
                + "\"id\":\"t_line\","
                + "\"name\":\"line\","
                + "\"symbol\":\"LINE_XOXO\","
                + "\"color\":\"white\","
                + "\"move_cost\":0"
                + "}"
        );
        final TerrainRegistry registry = new TerrainRegistry();

        TerrainParser.parseInto(jo, "core", registry);

        assertEquals("LINE_XOXO", registry.find("t_line").orElseThrow().getSymbol());
    }

    @Test
    void usesFirstSeasonalSymbolEntry() {
        final JsonValue jo = new JsonReader().parse(
            "{"
                + "\"type\":\"terrain\","
                + "\"id\":\"t_seasonal\","
                + "\"name\":\"seasonal\","
                + "\"symbol\":[\"+\",\"*\"],"
                + "\"color\":\"green\","
                + "\"move_cost\":2"
                + "}"
        );
        final TerrainRegistry registry = new TerrainRegistry();

        TerrainParser.parseInto(jo, "core", registry);

        assertEquals("+", registry.find("t_seasonal").orElseThrow().getSymbol());
    }

    @Test
    void skipsInvalidOrMissingIdTerrainObjects() {
        final JsonValue noId = new JsonReader().parse(
            "{"
                + "\"type\":\"terrain\","
                + "\"name\":\"missing id\""
                + "}"
        );
        final JsonValue wrongType = new JsonReader().parse(
            "{"
                + "\"type\":\"furniture\","
                + "\"id\":\"f_test\""
                + "}"
        );
        final TerrainRegistry registry = new TerrainRegistry();

        TerrainParser.parseInto(noId, "core", registry);
        TerrainParser.parseInto(wrongType, "core", registry);

        assertTrue(registry.allIds().isEmpty());
    }
}
