package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateMapgenApplierTest {

    @Test
    void updateOverlayPatchesBaseCell() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition base = ctx.catalog.findByOmTerrain("test_update_base").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(3L);

        final MapGrid grid = JsonMapgenRunner.run(base, ctx.catalog, ctx.palettes, options);
        assertEquals("t_floor", grid.get(1, 1).getTerrainId());

        UpdateMapgenApplier.mergeUpdate(grid, "test_update_patch", ctx.catalog, ctx.palettes, options, new Random(3L));

        assertEquals("t_dirt", grid.get(1, 1).getTerrainId());
        assertEquals("t_wall", grid.get(0, 0).getTerrainId());
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
