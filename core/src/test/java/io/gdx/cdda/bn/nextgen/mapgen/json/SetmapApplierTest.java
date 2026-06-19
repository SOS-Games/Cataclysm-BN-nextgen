package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetmapApplierTest {

    @Test
    void pointTerrainScattersWithStableSeed() {
        final MapGrid grid = new MapGrid(24, 24, "t_grass");
        final JsonValue set = new JsonReader().parse(
            "["
                + "{"
                + "\"point\":\"terrain\","
                + "\"id\":\"t_dirt\","
                + "\"x\":[0,23],"
                + "\"y\":[0,23],"
                + "\"repeat\":[50,50]"
                + "}"
                + "]"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(42L);
        final Random rng = new Random(42L);

        SetmapApplier.apply(grid, set, options, rng);

        assertEquals(48, countTerrain(grid, "t_dirt"));

        final MapGrid again = new MapGrid(24, 24, "t_grass");
        SetmapApplier.apply(again, set, options, new Random(42L));
        assertEquals(48, countTerrain(again, "t_dirt"));
    }

    @Test
    void squareTerrainFillsBlock() {
        final MapGrid grid = new MapGrid(24, 24, "t_grass");
        final JsonValue set = new JsonReader().parse(
            "["
                + "{"
                + "\"square\":\"terrain\","
                + "\"id\":\"t_wall\","
                + "\"x\":10,"
                + "\"y\":10,"
                + "\"x2\":12,"
                + "\"y2\":12"
                + "}"
                + "]"
        );

        SetmapApplier.apply(grid, set, new JsonMapgenRunOptions(), new Random(1L));

        for (int y = 10; y <= 12; y++) {
            for (int x = 10; x <= 12; x++) {
                assertEquals("t_wall", grid.get(x, y).getTerrainId());
            }
        }
    }

    @Test
    void lineTerrainDrawsCells() {
        final MapGrid grid = new MapGrid(5, 5, "t_grass");
        final JsonValue set = new JsonReader().parse(
            "["
                + "{"
                + "\"line\":\"terrain\","
                + "\"id\":\"t_dirt\","
                + "\"x\":0,"
                + "\"y\":0,"
                + "\"x2\":4,"
                + "\"y2\":4"
                + "}"
                + "]"
        );

        SetmapApplier.apply(grid, set, new JsonMapgenRunOptions(), new Random(1L));

        assertEquals("t_dirt", grid.get(0, 0).getTerrainId());
        assertEquals("t_dirt", grid.get(4, 4).getTerrainId());
        assertEquals("t_grass", grid.get(0, 4).getTerrainId());
    }

    @Test
    void chanceSkipsEntireEntry() {
        final MapGrid grid = new MapGrid(3, 3, "t_grass");
        final JsonValue set = new JsonReader().parse(
            "["
                + "{"
                + "\"point\":\"terrain\","
                + "\"id\":\"t_dirt\","
                + "\"x\":1,"
                + "\"y\":1,"
                + "\"repeat\":1,"
                + "\"chance\":100"
                + "}"
                + "]"
        );
        final Random rng = new Random(7L);

        SetmapApplier.apply(grid, set, new JsonMapgenRunOptions(), rng);

        assertEquals(0, countTerrain(grid, "t_dirt"));
    }

    @Test
    void trapEntryAddsWarning() {
        final MapGrid grid = new MapGrid(3, 3, "t_grass");
        final JsonValue set = new JsonReader().parse(
            "[{\"point\":\"trap\",\"id\":\"tr_pit\",\"x\":1,\"y\":1}]"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions();

        SetmapApplier.apply(grid, set, options, new Random(1L));

        assertTrue(options.getWarnings().stream().anyMatch(w -> w.contains("trap")));
    }

    private static int countTerrain(final MapGrid grid, final String terrainId) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (terrainId.equals(grid.get(x, y).getTerrainId())) {
                    count++;
                }
            }
        }
        return count;
    }
}
