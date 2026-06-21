package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedMapgenRunnerTest {

    @Test
    void nestedRoomChunkAtOffset() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition parent = ctx.catalog.findByOmTerrain("test_nested_parent").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(42L);

        final MapGrid grid = JsonMapgenRunner.run(parent, ctx.catalog, ctx.palettes, options);

        assertEquals("t_grass", grid.get(0, 0).getTerrainId());
        assertEquals("t_wall", grid.get(6, 6).getTerrainId());
        assertEquals("t_floor", grid.get(7, 7).getTerrainId());
        assertEquals("t_wall", grid.get(17, 17).getTerrainId());
        assertEquals("t_grass", grid.get(23, 23).getTerrainId());
    }

    @Test
    void nullChunkSkipsPlacement() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final MapGrid parent = new MapGrid(8, 8, "t_grass");
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewSeed(1L)
            .withMapgenCatalog(ctx.catalog);
        final com.badlogic.gdx.utils.JsonValue object = new com.badlogic.gdx.utils.JsonReader().parse(
            "{"
                + "\"place_nested\":[{"
                + "\"chunks\":[[\"null\",1]],"
                + "\"x\":2,"
                + "\"y\":2"
                + "}]"
                + "}"
        );

        NestedMapgenRunner.apply(
            parent,
            object,
            java.util.List.of(),
            ctx.catalog,
            ctx.palettes,
            options,
            new Random(1L),
            0
        );

        assertEquals("t_grass", parent.get(2, 2).getTerrainId());
    }

    @Test
    void unknownChunkAddsWarning() {
        final MapGrid parent = new MapGrid(8, 8, "t_grass");
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(1L);
        final com.badlogic.gdx.utils.JsonValue object = new com.badlogic.gdx.utils.JsonReader().parse(
            "{"
                + "\"place_nested\":[{"
                + "\"chunks\":[[\"missing_chunk\",1]],"
                + "\"x\":2,"
                + "\"y\":2"
                + "}]"
                + "}"
        );

        NestedMapgenRunner.apply(
            parent,
            object,
            java.util.List.of(),
            new MapgenCatalog(java.util.List.of()),
            new PaletteRegistry(),
            options,
            new Random(1L),
            0
        );

        assertEquals("t_grass", parent.get(2, 2).getTerrainId());
        assertTrue(options.getWarnings().stream().anyMatch(w -> w.contains("missing_chunk")));
    }

    @Test
    void catalogIndexesNestedMapgenId() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        assertEquals(1, ctx.catalog.findByNestedMapgenId("test_nested_room").size());
        assertTrue(ctx.catalog.pickNestedMapgen("test_nested_room", new Random(1L)).isPresent());
    }

    @Test
    void wrongNeighborUsesElseChunk() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition parent = ctx.catalog.findByOmTerrain("test_nested_neighbor_parent").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewSeed(11L)
            .withNeighborsByDirection(neighbors("field", "field", "field", "field"));

        final MapGrid grid = JsonMapgenRunner.run(parent, ctx.catalog, ctx.palettes, options);

        assertEquals("t_rock_floor", grid.get(6, 6).getTerrainId());
    }

    @Test
    void matchingNeighborUsesPrimaryChunk() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition parent = ctx.catalog.findByOmTerrain("test_nested_neighbor_parent").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewSeed(11L)
            .withNeighborsByDirection(neighbors("lab_north", "field", "field", "field"));

        final MapGrid grid = JsonMapgenRunner.run(parent, ctx.catalog, ctx.palettes, options);

        assertEquals("t_wall", grid.get(6, 6).getTerrainId());
    }

    private static Map<String, String> neighbors(
        final String north,
        final String east,
        final String south,
        final String west
    ) {
        final Map<String, String> neighbors = new HashMap<>();
        neighbors.put("north", north);
        neighbors.put("east", east);
        neighbors.put("south", south);
        neighbors.put("west", west);
        return neighbors;
    }

    private static FixtureContext loadFixtureContext() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        return new FixtureContext(
            PaletteLoader.load(options).getPalettes(),
            JsonMapgenLoader.load(options).getCatalog()
        );
    }

    private static final class FixtureContext {
        private final PaletteRegistry palettes;
        private final MapgenCatalog catalog;

        private FixtureContext(final PaletteRegistry palettes, final MapgenCatalog catalog) {
            this.palettes = palettes;
            this.catalog = catalog;
        }
    }
}
