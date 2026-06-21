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
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapgenParameterRunnerTest {

    @Test
    void parametersResolveTerrainFromRolledValue() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_parameters_room").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(7L);

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.catalog, ctx.palettes, options);

        assertEquals("t_flat_roof", grid.get(0, 0).getTerrainId());
        assertTrue(options.getWarnings().isEmpty());
    }

    @Test
    void paramLookupUsesRolledParametersMap() {
        final com.badlogic.gdx.utils.JsonValue value = new com.badlogic.gdx.utils.JsonReader().parse(
            "{ \"param\": \"roof_type\", \"fallback\": \"t_floor\" }"
        );
        final Map<String, String> rolled = new HashMap<>();
        rolled.put("roof_type", "t_tar_flat_roof");

        final String resolved = io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteCharResolver
            .resolve(value, null, null, rolled)
            .orElseThrow();

        assertEquals("t_tar_flat_roof", resolved);
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
