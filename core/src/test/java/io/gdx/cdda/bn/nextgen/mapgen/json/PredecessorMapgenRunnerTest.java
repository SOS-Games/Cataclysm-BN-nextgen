package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonReader;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredecessorMapgenRunnerTest {

    @Test
    void houseOnFieldKeepsGrassOutsideRoom() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_house_on_field").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions();

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.catalog, ctx.palettes, options);

        assertEquals(OmtStitchComposer.DEFAULT_OMT_SIZE, grid.width());
        assertEquals(OmtStitchComposer.DEFAULT_OMT_SIZE, grid.height());
        assertEquals("t_wall", grid.get(0, 0).getTerrainId());
        assertEquals("t_floor", grid.get(1, 1).getTerrainId());
        assertEquals("t_grass", grid.get(10, 10).getTerrainId());
        assertEquals("t_grass", grid.get(23, 23).getTerrainId());
    }

    @Test
    void unknownPredecessorWarnsAndUsesFillTer() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = JsonMapgenParser.parse(
            new JsonReader().parse(
                "{"
                    + "\"type\":\"mapgen\","
                    + "\"method\":\"json\","
                    + "\"om_terrain\":\"test_missing_pred\","
                    + "\"object\":{"
                    + "\"predecessor_mapgen\":\"missing_bg\","
                    + "\"fill_ter\":\"t_dirt\","
                    + "\"rows\":[\"###\"]"
                    + "}"
                    + "}"
            ),
            Paths.get("inline.json"),
            0
        ).orElseThrow();
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions();

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.catalog, ctx.palettes, options);

        assertTrue(options.getWarnings().stream().anyMatch(w -> w.contains("missing_bg")));
        assertEquals(OmtStitchComposer.DEFAULT_OMT_SIZE, grid.width());
        assertEquals("t_dirt", grid.get(20, 20).getTerrainId());
    }

    @Test
    void depthLimitWarnsOnLongChain() {
        final PaletteRegistry palettes = new PaletteRegistry();
        final List<JsonMapgenDefinition> definitions = buildDepthChainDefinitions();
        final MapgenCatalog catalog = new MapgenCatalog(definitions);
        final JsonMapgenDefinition top = definitions.get(definitions.size() - 1);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions();

        JsonMapgenRunner.run(top, catalog, palettes, options);

        assertTrue(
            options.getWarnings().stream().anyMatch(w -> w.contains("depth limit")),
            () -> "warnings: " + options.getWarnings()
        );
    }

    private static List<JsonMapgenDefinition> buildDepthChainDefinitions() {
        final List<JsonMapgenDefinition> definitions = new ArrayList<>();
        for (int i = 0; i <= PredecessorMapgenRunner.MAX_DEPTH + 1; i++) {
            final String id = "pred_depth_" + i;
            final String predecessor = i == 0 ? null : "pred_depth_" + (i - 1);
            final StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"type\":\"mapgen\",");
            json.append("\"method\":\"json\",");
            json.append("\"om_terrain\":\"").append(id).append("\",");
            json.append("\"object\":{");
            if (predecessor != null) {
                json.append("\"predecessor_mapgen\":\"").append(predecessor).append("\",");
            }
            json.append("\"fill_ter\":\"t_dirt\",");
            json.append("\"rows\":[\".\"]");
            json.append("}");
            json.append("}");
            definitions.add(JsonMapgenParser.parse(
                new JsonReader().parse(json.toString()),
                Paths.get("inline.json"),
                i
            ).orElseThrow());
        }
        return definitions;
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
