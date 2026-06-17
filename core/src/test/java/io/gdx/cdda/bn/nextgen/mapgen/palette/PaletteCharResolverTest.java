package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteCharResolverTest {

    @Test
    void resolvesPlainString() {
        final JsonValue value = new JsonReader().parse("\"t_brick_wall\"");
        assertEquals("t_brick_wall", PaletteCharResolver.resolveId(value).orElseThrow());
    }

    @Test
    void resolvesFirstStringInArray() {
        final JsonValue value = new JsonReader().parse("[ \"f_indoor_plant\", \"f_indoor_plant_y\" ]");
        assertEquals("f_indoor_plant", PaletteCharResolver.resolveId(value).orElseThrow());
    }

    @Test
    void resolvesFirstWeightedEntry() {
        final JsonValue value = new JsonReader().parse(
            "[ [ \"t_door_c\", 5 ], [ \"t_door_o\", 5 ], \"t_door_locked_interior\" ]"
        );
        assertEquals("t_door_c", PaletteCharResolver.resolveId(value).orElseThrow());
    }

    @Test
    void resolvesParamFallback() {
        final JsonValue value = new JsonReader().parse(
            "{ \"param\": \"interior_wall_type\", \"fallback\": \"t_wall_w\" }"
        );
        assertEquals("t_wall_w", PaletteCharResolver.resolveId(value).orElseThrow());
    }

    @Test
    void emptyArrayReturnsEmpty() {
        final JsonValue value = new JsonReader().parse("[]");
        assertTrue(PaletteCharResolver.resolveId(value).isEmpty());
    }
}
