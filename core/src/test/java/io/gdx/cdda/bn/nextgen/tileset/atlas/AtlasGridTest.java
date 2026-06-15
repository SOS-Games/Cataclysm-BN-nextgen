package io.gdx.cdda.bn.nextgen.tileset.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtlasGridTest {

    @Test
    void sizeFor320x240With32Cells() {
        assertEquals(70, AtlasGrid.expectedTileCount(320, 240, 32, 32));
    }

    @Test
    void sizeDiscardsRemainderPixels() {
        assertEquals(9, AtlasGrid.expectedTileCount(100, 100, 32, 32));
    }

    @Test
    void nonSquareCellsOnWideSheet() {
        assertEquals(20, AtlasGrid.expectedTileCount(200, 40, 20, 20));
    }

    @Test
    void localIndexZeroIsTopLeft() {
        assertEquals(0, AtlasGrid.localIndex(0, 0, 320, 32, 32));
    }

    @Test
    void globalIndexAddsOffset() {
        assertEquals(121, AtlasGrid.globalIndex(32, 64, 320, 32, 32, 100));
    }

    @Test
    void divideRoundUp() {
        assertEquals(2, AtlasGrid.divideRoundUp(513, 512));
        assertEquals(1, AtlasGrid.divideRoundUp(512, 512));
    }

    @Test
    void rejectsZeroDivisor() {
        assertThrows(IllegalArgumentException.class, () -> AtlasGrid.divideRoundUp(10, 0));
    }
}
