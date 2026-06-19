package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmapgenIntRangeTest {

    @Test
    void rollFixedInt() {
        final JsonValue field = new JsonReader().parse("7");
        assertEquals(7, JmapgenIntRange.roll(field, new Random(1)));
    }

    @Test
    void rollInclusiveRange() {
        final JsonValue field = new JsonReader().parse("[3, 5]");
        final Random rng = new Random(42);
        for (int i = 0; i < 20; i++) {
            final int rolled = JmapgenIntRange.roll(field, rng);
            assertTrue(rolled >= 3 && rolled <= 5);
        }
    }

    @Test
    void rollOptionalUsesDefaultWhenMissing() {
        final JsonValue object = new JsonReader().parse("{}");
        assertEquals(9, JmapgenIntRange.rollOptional(object, "repeat", 9, new Random(1)));
    }
}
