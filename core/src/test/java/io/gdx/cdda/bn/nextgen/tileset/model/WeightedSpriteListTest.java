package io.gdx.cdda.bn.nextgen.tileset.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class WeightedSpriteListTest {

    @Test
    void pickByIndexMatchesPrecalcTable() {
        final WeightedSpriteList list = new WeightedSpriteList();
        list.add(List.of(10), 3);
        list.add(List.of(20), 1);
        list.add(List.of(30), 5);
        list.precalc();

        assertEquals(10, list.pickByIndex(0).getFrame(0));
        assertEquals(10, list.pickByIndex(2).getFrame(0));
        assertEquals(20, list.pickByIndex(3).getFrame(0));
        assertEquals(30, list.pickByIndex(4).getFrame(0));
        assertEquals(30, list.pickByIndex(8).getFrame(0));
        assertEquals(10, list.pickByIndex(9).getFrame(0));
    }

    @Test
    void pickByIndexSingleVariant() {
        final WeightedSpriteList list = new WeightedSpriteList();
        list.add(List.of(7, 8), 4);
        final SpriteVariant variant = list.pickByIndex(100);
        assertSame(list.getVariants().get(0), variant);
        assertEquals(7, variant.getFrame(0));
    }

    @Test
    void pickByIndexNegativeRandi() {
        final WeightedSpriteList list = new WeightedSpriteList();
        list.add(List.of(10), 3);
        list.add(List.of(20), 1);
        list.add(List.of(30), 5);
        list.precalc();

        assertNotNull(list.pickByIndex(-4));
        assertEquals(list.pickByIndex(5).getFrame(0), list.pickByIndex(-4).getFrame(0));
    }
}
