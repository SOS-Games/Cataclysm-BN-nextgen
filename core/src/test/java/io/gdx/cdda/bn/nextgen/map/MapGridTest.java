package io.gdx.cdda.bn.nextgen.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapGridTest {

    @Test
    void createsGridWithFillTerrain() {
        final MapGrid grid = new MapGrid(4, 4, "t_dirt");

        assertEquals(4, grid.width());
        assertEquals(4, grid.height());
        assertEquals("t_dirt", grid.get(0, 0).getTerrainId());
        assertEquals("t_dirt", grid.get(3, 3).getTerrainId());
    }

    @Test
    void resizePreservesTopLeftOverlap() {
        final MapGrid grid = new MapGrid(4, 4, "t_dirt");
        grid.setTerrain(0, 0, "t_grass");
        grid.setTerrain(3, 3, "t_floor");
        grid.resize(6, 6, "t_rock");

        assertEquals(6, grid.width());
        assertEquals(6, grid.height());
        assertEquals("t_grass", grid.get(0, 0).getTerrainId());
        assertEquals("t_floor", grid.get(3, 3).getTerrainId());
        assertEquals("t_rock", grid.get(5, 5).getTerrainId());
    }

    @Test
    void fillReplacesAllCells() {
        final MapGrid grid = new MapGrid(3, 3, "t_dirt");
        grid.setTerrain(1, 1, "t_grass");
        grid.fill("t_concrete");

        assertEquals("t_concrete", grid.get(0, 0).getTerrainId());
        assertEquals("t_concrete", grid.get(1, 1).getTerrainId());
        assertEquals("t_concrete", grid.get(2, 2).getTerrainId());
    }

    @Test
    void outOfBoundsThrows() {
        final MapGrid grid = new MapGrid(2, 2, "t_dirt");
        assertThrows(IndexOutOfBoundsException.class, () -> grid.get(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.setTerrain(-1, 0, "t_dirt"));
    }
}
