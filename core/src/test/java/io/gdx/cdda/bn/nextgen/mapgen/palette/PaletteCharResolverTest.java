package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteCharResolverTest {

    @Test
    void resolvesPlainString() {
        final JsonValue value = new JsonReader().parse("\"t_brick_wall\"");
        assertEquals("t_brick_wall", PaletteCharResolver.resolveId(value).orElseThrow());
    }

    @Test
    void resolvesFirstStringInArrayWithoutRng() {
        final JsonValue value = new JsonReader().parse("[ \"f_indoor_plant\", \"f_indoor_plant_y\" ]");
        assertEquals("f_indoor_plant", PaletteCharResolver.resolveId(value).orElseThrow());
    }

    @Test
    void resolvesWeightedArrayWithSeed() {
        final JsonValue value = new JsonReader().parse(
            "[ [ \"t_door_c\", 5 ], [ \"t_door_o\", 5 ], \"t_door_locked_interior\" ]"
        );
        final Random rng = new Random(99L);
        final String first = PaletteCharResolver.resolve(value, rng, null).orElseThrow();
        final String second = PaletteCharResolver.resolve(value, new Random(99L), null).orElseThrow();
        assertEquals(first, second);
        assertTrue(
            first.equals("t_door_c")
                || first.equals("t_door_o")
                || first.equals("t_door_locked_interior")
        );
    }

    @Test
    void uniformStringArrayPicksAmongEntries() {
        final JsonValue value = new JsonReader().parse("[ \"f_indoor_plant\", \"f_indoor_plant_y\" ]");
        final String picked = PaletteCharResolver.resolve(value, new Random(3L), null).orElseThrow();
        assertTrue(picked.equals("f_indoor_plant") || picked.equals("f_indoor_plant_y"));
    }

    @Test
    void resolvesParamFallback() {
        final JsonValue value = new JsonReader().parse(
            "{ \"param\": \"interior_wall_type\", \"fallback\": \"t_wall_w\" }"
        );
        assertEquals("t_wall_w", PaletteCharResolver.resolve(value, new Random(1L), null).orElseThrow());
    }

    @Test
    void emptyArrayReturnsEmpty() {
        final JsonValue value = new JsonReader().parse("[]");
        assertTrue(PaletteCharResolver.resolveId(value).isEmpty());
    }
}
