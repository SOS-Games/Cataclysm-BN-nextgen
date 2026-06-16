package io.gdx.cdda.bn.nextgen.gamedata;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.FurnitureParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FurnitureParserTest {

    @Test
    void parsesV1FieldsForFurnitureObject() {
        final JsonValue jo = new JsonReader().parse(
            "{"
                + "\"type\":\"furniture\","
                + "\"id\":\"f_rubble\","
                + "\"name\":\"rubble\","
                + "\"symbol\":\"#\","
                + "\"color\":\"brown\","
                + "\"move_cost_mod\":2,"
                + "\"required_str\":10,"
                + "\"flags\":[\"TRANSPARENT\",\"SHORT\"],"
                + "\"looks_like\":\"f_null\""
                + "}"
        );
        final FurnitureRegistry registry = new FurnitureRegistry();

        FurnitureParser.parseInto(jo, "core", registry);

        final FurnitureDefinition furniture = registry.find("f_rubble").orElseThrow();
        assertEquals("rubble", furniture.getName());
        assertEquals("#", furniture.getSymbol());
        assertEquals("brown", furniture.getColor());
        assertEquals(2, furniture.getMoveCostMod());
        assertEquals(10, furniture.getRequiredStr());
        assertEquals("f_null", furniture.getLooksLike());
        assertEquals("core", furniture.getSourceMod());
    }

    @Test
    void furnitureUsesMoveCostModFieldNotTerrainMoveCost() {
        final JsonValue jo = new JsonReader().parse(
            "{"
                + "\"type\":\"furniture\","
                + "\"id\":\"f_fixture\","
                + "\"name\":\"fixture\","
                + "\"symbol\":\"f\","
                + "\"color\":\"white\","
                + "\"move_cost\":9,"
                + "\"move_cost_mod\":3"
                + "}"
        );
        final FurnitureRegistry registry = new FurnitureRegistry();

        FurnitureParser.parseInto(jo, "core", registry);

        assertEquals(3, registry.find("f_fixture").orElseThrow().getMoveCostMod());
    }

    @Test
    void secondDefinitionOverridesFirstById() {
        final JsonValue first = new JsonReader().parse(
            "{"
                + "\"type\":\"furniture\","
                + "\"id\":\"f_fixture\","
                + "\"name\":\"first\","
                + "\"move_cost_mod\":1"
                + "}"
        );
        final JsonValue second = new JsonReader().parse(
            "{"
                + "\"type\":\"furniture\","
                + "\"id\":\"f_fixture\","
                + "\"name\":\"second\","
                + "\"move_cost_mod\":4"
                + "}"
        );
        final FurnitureRegistry registry = new FurnitureRegistry();

        FurnitureParser.parseInto(first, "core", registry);
        FurnitureParser.parseInto(second, "core", registry);

        final FurnitureDefinition furniture = registry.find("f_fixture").orElseThrow();
        assertEquals("second", furniture.getName());
        assertEquals(4, furniture.getMoveCostMod());
    }

    @Test
    void skipsInvalidOrMissingIdFurnitureObjects() {
        final JsonValue noId = new JsonReader().parse(
            "{"
                + "\"type\":\"furniture\","
                + "\"name\":\"missing id\""
                + "}"
        );
        final JsonValue wrongType = new JsonReader().parse(
            "{"
                + "\"type\":\"terrain\","
                + "\"id\":\"t_test\""
                + "}"
        );
        final FurnitureRegistry registry = new FurnitureRegistry();

        FurnitureParser.parseInto(noId, "core", registry);
        FurnitureParser.parseInto(wrongType, "core", registry);

        assertTrue(registry.allIds().isEmpty());
    }
}
