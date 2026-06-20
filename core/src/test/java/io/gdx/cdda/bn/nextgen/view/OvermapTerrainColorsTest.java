package io.gdx.cdda.bn.nextgen.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OvermapTerrainColorsTest {

    @Test
    void resolvesNamedBnColors() {
        assertEquals(OvermapTerrainColors.resolve("brown", "test_field"), OvermapTerrainColors.resolve("brown", "x"));
    }

    @Test
    void hashFallbackIsStable() {
        assertEquals(
            OvermapTerrainColors.hashColor("unknown_omt"),
            OvermapTerrainColors.hashColor("unknown_omt")
        );
        assertNotEquals(
            OvermapTerrainColors.hashColor("a"),
            OvermapTerrainColors.hashColor("b")
        );
    }
}
