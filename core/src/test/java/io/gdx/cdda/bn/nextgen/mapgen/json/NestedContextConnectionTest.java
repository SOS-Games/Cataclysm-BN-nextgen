package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NestedContextConnectionTest {

    @Test
    void wrongConnectionUsesElseChunk() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition parent = ctx.catalog.findByOmTerrain("test_nested_connection_parent").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewSeed(21L)
            .withConnectionsByDirection(connections("test_field", "test_field", "test_field", "test_field"));

        final MapGrid grid = JsonMapgenRunner.run(parent, ctx.catalog, ctx.palettes, options);

        assertEquals("t_rock_floor", grid.get(6, 6).getTerrainId());
    }

    @Test
    void matchingConnectionUsesPrimaryChunk() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition parent = ctx.catalog.findByOmTerrain("test_nested_connection_parent").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewSeed(21L)
            .withConnectionsByDirection(connections("test_local_road", "test_field", "test_field", "test_field"));

        final MapGrid grid = JsonMapgenRunner.run(parent, ctx.catalog, ctx.palettes, options);

        assertEquals("t_wall", grid.get(6, 6).getTerrainId());
    }

    private static Map<String, String> connections(
        final String north,
        final String east,
        final String south,
        final String west
    ) {
        final Map<String, String> connections = new HashMap<>();
        connections.put("north", north);
        connections.put("east", east);
        connections.put("south", south);
        connections.put("west", west);
        return connections;
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
