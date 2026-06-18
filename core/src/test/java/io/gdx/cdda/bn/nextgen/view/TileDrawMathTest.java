package io.gdx.cdda.bn.nextgen.view;

import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileDrawMathTest {

    @Test
    void tallSpriteWithNegativeOffsetAnchorsTrunkToCellBottom() {
        final TileDefinition tile = new TileDefinition("t_tree_dead");
        tile.setOffsetX(-16);
        tile.setOffsetY(-48);

        final float cellBottom = 100f;
        final float tilePx = 32f;

        final TileDrawMath.DrawRect rect = TileDrawMath.computeDrawRect(
            32,
            32,
            tile,
            64,
            80,
            0f,
            cellBottom,
            tilePx,
            0
        );

        assertEquals(-16f, rect.x, 0.001f);
        assertEquals(cellBottom, rect.y, 0.001f);
        assertEquals(64f, rect.width, 0.001f);
        assertEquals(80f, rect.height, 0.001f);
    }

    @Test
    void standardSpriteFillsCellWhenOffsetsAreZero() {
        final TileDefinition tile = new TileDefinition("t_grass");

        final float cellBottom = 50f;
        final float tilePx = 32f;

        final TileDrawMath.DrawRect rect = TileDrawMath.computeDrawRect(
            32,
            32,
            tile,
            32,
            32,
            10f,
            cellBottom,
            tilePx,
            0
        );

        assertEquals(10f, rect.x, 0.001f);
        assertEquals(cellBottom, rect.y, 0.001f);
        assertEquals(32f, rect.width, 0.001f);
        assertEquals(32f, rect.height, 0.001f);
    }
}
