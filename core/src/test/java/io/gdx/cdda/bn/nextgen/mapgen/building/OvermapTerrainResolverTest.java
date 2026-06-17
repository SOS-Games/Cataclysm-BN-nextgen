package io.gdx.cdda.bn.nextgen.mapgen.building;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OvermapTerrainResolverTest {

    @Test
    void stripsRotationSuffixes() {
        assertEquals("house_09", OvermapTerrainResolver.stripRotation("house_09_north"));
        assertEquals("house_09_roof", OvermapTerrainResolver.stripRotation("house_09_roof_north"));
        assertEquals(
            "2StoryModern04_1_2",
            OvermapTerrainResolver.stripRotation("2StoryModern04_1_2_north")
        );
        assertEquals("test_room", OvermapTerrainResolver.stripRotation("test_room"));
    }
}
