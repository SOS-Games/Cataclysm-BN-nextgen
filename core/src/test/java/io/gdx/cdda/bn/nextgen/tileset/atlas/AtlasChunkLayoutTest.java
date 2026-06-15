package io.gdx.cdda.bn.nextgen.tileset.atlas;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtlasChunkLayoutTest {

    @Test
    void singleChunkWhenAtlasFitsMaxTexture() {
        final List<AtlasChunkLayout.ChunkRect> chunks = AtlasChunkLayout.computeChunks(
            64, 64, 32, 32, 4096, 4096
        );
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).getX());
        assertEquals(64, chunks.get(0).getWidth());
    }

    @Test
    void splitsWideAtlasAcrossChunks() {
        final List<AtlasChunkLayout.ChunkRect> chunks = AtlasChunkLayout.computeChunks(
            512, 64, 32, 32, 256, 4096
        );
        assertEquals(2, chunks.size());
        assertEquals(0, chunks.get(0).getX());
        assertEquals(256, chunks.get(0).getWidth());
        assertEquals(256, chunks.get(1).getX());
        assertEquals(256, chunks.get(1).getWidth());
    }

    @Test
    void rejectsMaxTextureSmallerThanCell() {
        assertThrows(IllegalArgumentException.class, () ->
            AtlasChunkLayout.computeChunks(64, 64, 32, 32, 16, 4096)
        );
    }
}
