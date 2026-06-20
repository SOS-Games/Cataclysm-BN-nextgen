package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RiverGeneratorTest {

    @Test
    void carvesAtLeastThreeRiverCellsOn16x16() {
        final OvermapGrid grid = new OvermapGrid(16, 16, "test_field");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(55L)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "test_local_road", "test_river", "test_river");
        final int carved = RiverGenerator.carve(
            grid,
            options,
            null,
            new Random(55L),
            new ArrayList<>()
        );
        assertTrue(carved >= 3, "expected at least three river cells, got " + carved);

        int riverCells = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if ("test_river".equals(grid.getOmtId(x, y))) {
                    riverCells++;
                }
            }
        }
        assertTrue(riverCells >= 3);
    }
}
