package io.gdx.cdda.bn.nextgen.worldgen.region;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionGroundcoverSettingsTest {

    @Test
    void parsesWeightedArrayPairs() {
        final JsonValue root = new JsonReader().parse("[[\"t_dirt\", 3], [\"t_grass\", 1]]");
        final RegionGroundcoverSettings settings = RegionGroundcoverSettings.parse(root);

        assertTrue(settings.isWeighted());
        assertEquals("t_dirt", settings.getDefaultTerrainId());
        final Set<String> seen = new HashSet<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                seen.add(settings.pickAt(7L, x, y));
            }
        }
        assertTrue(seen.contains("t_dirt"));
        assertTrue(seen.contains("t_grass"));
    }

    @Test
    void pickAtIsStablePerCell() {
        final JsonValue root = new JsonReader().parse("[[\"t_dirt\", 1], [\"t_grass\", 1]]");
        final RegionGroundcoverSettings settings = RegionGroundcoverSettings.parse(root);
        final String first = settings.pickAt(99L, 4, 7);
        assertEquals(first, settings.pickAt(99L, 4, 7));
        assertFalse(settings.pickAt(99L, 0, 0).isEmpty());
    }

    @Test
    void weightedFieldVisitUsesMultipleGroundcovers() {
        final JsonValue root = new JsonReader().parse("[[\"t_dirt\", 3], [\"t_grass\", 1]]");
        final RegionGroundcoverSettings settings = RegionGroundcoverSettings.parse(root);
        final Set<String> seen = new HashSet<>();
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 24; x++) {
                seen.add(settings.pickAt(4242L, x, y));
            }
        }
        assertTrue(seen.contains("t_dirt"));
        assertTrue(seen.contains("t_grass"));
    }
}
