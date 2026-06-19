package io.gdx.cdda.bn.nextgen.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapGridRotatorTest {

    @Test
    void rotate90ClockwiseMovesAsymmetricLandmark() {
        final MapGrid source = asymmetricGrid();

        final MapGrid rotated = MapGridRotator.rotate(source, 1);

        assertEquals(3, rotated.width());
        assertEquals(5, rotated.height());
        assertEquals("t_wall", rotated.get(2, 0).getTerrainId());
        assertEquals("t_floor", rotated.get(0, 0).getTerrainId());
    }

    @Test
    void rotate180And270And360() {
        final MapGrid source = asymmetricGrid();

        final MapGrid rotated180 = MapGridRotator.rotate(source, 2);
        assertEquals(5, rotated180.width());
        assertEquals(3, rotated180.height());
        assertEquals("t_wall", rotated180.get(4, 2).getTerrainId());

        final MapGrid rotated270 = MapGridRotator.rotate(source, 3);
        assertEquals(3, rotated270.width());
        assertEquals(5, rotated270.height());
        assertEquals("t_wall", rotated270.get(0, 4).getTerrainId());

        assertEquals(source, MapGridRotator.rotate(source, 4));
        assertEquals(source, MapGridRotator.rotate(source, 0));
    }

    @Test
    void rotateCopiesFurniture() {
        final MapGrid source = new MapGrid(2, 2, "t_floor");
        source.setFurniture(0, 1, "f_chair");

        final MapGrid rotated = MapGridRotator.rotate(source, 1);

        assertEquals("f_chair", rotated.get(0, 0).getFurnitureId());
        assertNull(rotated.get(1, 0).getFurnitureId());
    }

    @Test
    void rotationFromOmSuffixMatchesCardinals() {
        assertEquals(0, MapGridRotator.rotationFromOmSuffix("house_north"));
        assertEquals(1, MapGridRotator.rotationFromOmSuffix("house_east"));
        assertEquals(2, MapGridRotator.rotationFromOmSuffix("house_south"));
        assertEquals(3, MapGridRotator.rotationFromOmSuffix("house_west"));
        assertEquals(0, MapGridRotator.rotationFromOmSuffix("house"));
        assertEquals(0, MapGridRotator.rotationFromOmSuffix("wing_north_east"));
    }

    private static MapGrid asymmetricGrid() {
        final MapGrid grid = new MapGrid(5, 3, "t_floor");
        grid.setTerrain(0, 0, "t_wall");
        return grid;
    }
}
