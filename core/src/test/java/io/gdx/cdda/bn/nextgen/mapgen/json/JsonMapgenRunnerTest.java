package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteLoader;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JsonMapgenRunnerTest {

    @Test
    void simpleRoomProducesExpectedGrid() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_room").get(0);

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, new JsonMapgenRunOptions());

        assertEquals(5, grid.width());
        assertEquals(5, grid.height());
        assertEquals("t_wall", grid.get(0, 0).getTerrainId());
        assertEquals("t_floor", grid.get(1, 1).getTerrainId());
        assertEquals("f_chair", grid.get(2, 2).getFurnitureId());
        assertEquals("t_wall", grid.get(4, 4).getTerrainId());
    }

    @Test
    void inlineTerrainOverridesPalette() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_override").get(0);

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, new JsonMapgenRunOptions());

        assertEquals(3, grid.width());
        assertEquals("t_custom_wall", grid.get(0, 0).getTerrainId());
        assertEquals("t_floor", grid.get(1, 1).getTerrainId());
    }

    @Test
    void unknownPaletteAddsWarningButStillFillsGrid() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = JsonMapgenParser.parse(
            new com.badlogic.gdx.utils.JsonReader().parse(
                "{"
                    + "\"type\":\"mapgen\","
                    + "\"method\":\"json\","
                    + "\"om_terrain\":\"test_bad_palette\","
                    + "\"object\":{"
                    + "\"fill_ter\":\"t_floor\","
                    + "\"palettes\":[\"missing_palette\",\"minimal\"],"
                    + "\"rows\":[\"#####\",\"#...#\"]"
                    + "}"
                    + "}"
            ),
            java.nio.file.Paths.get("inline.json"),
            0
        ).orElseThrow();
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions();

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, options);

        assertFalse(options.getWarnings().isEmpty());
        assertTrue(options.getWarnings().get(0).contains("missing_palette"));
        assertEquals("t_wall", grid.get(0, 0).getTerrainId());
    }

    @Test
    void setmapBufferFixtureProducesStableScatterAndWallBlock() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_setmap_buffer").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(42L);

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, options);

        assertEquals(24, grid.width());
        assertEquals(48, countTerrain(grid, "t_dirt"));
        for (int y = 10; y <= 12; y++) {
            for (int x = 10; x <= 12; x++) {
                assertEquals("t_wall", grid.get(x, y).getTerrainId());
            }
        }
    }

    @Test
    void placeFurnitureFixturePlacesChair() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_place_furniture").get(0);

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, new JsonMapgenRunOptions());

        assertEquals("t_floor", grid.get(2, 2).getTerrainId());
        assertEquals("f_chair", grid.get(2, 2).getFurnitureId());
    }

    @Test
    void placeMonstersWarnFixtureLeavesGridUnchanged() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_place_monsters_warn").get(0);
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(7L);

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, options);

        assertEquals("t_floor", grid.get(2, 2).getTerrainId());
        assertTrue(options.getWarnings().stream().anyMatch(w -> w.contains("monster spawns not shown")));
    }

    @Test
    void rotationFixtureMovesLandmark() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_rotation_asymmetric").get(0);

        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, new JsonMapgenRunOptions());

        assertEquals(3, grid.width());
        assertEquals(5, grid.height());
        assertEquals("t_wall", grid.get(2, 0).getTerrainId());
    }

    @Test
    void omtRotationChangesOutput() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = ctx.catalog.findByOmTerrain("test_rotation_asymmetric_base").get(0);

        final MapGrid unrotated = JsonMapgenRunner.run(definition, ctx.palettes, new JsonMapgenRunOptions());
        final MapGrid rotated = JsonMapgenRunner.run(
            definition,
            ctx.palettes,
            new JsonMapgenRunOptions().withOmtRotation(1)
        );

        assertEquals("t_wall", unrotated.get(0, 0).getTerrainId());
        assertEquals("t_wall", rotated.get(2, 0).getTerrainId());
        assertEquals(MapGridRotator.rotate(unrotated, 1).width(), rotated.width());
    }

    @Test
    void rowsOverwriteSetmapOnSameCell() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        final JsonMapgenDefinition definition = JsonMapgenParser.parse(
            new com.badlogic.gdx.utils.JsonReader().parse(
                "{"
                    + "\"type\":\"mapgen\","
                    + "\"method\":\"json\","
                    + "\"om_terrain\":\"test_setmap_rows_order\","
                    + "\"object\":{"
                    + "\"fill_ter\":\"t_floor\","
                    + "\"palettes\":[\"minimal\"],"
                    + "\"set\":[{\"point\":\"terrain\",\"id\":\"t_dirt\",\"x\":1,\"y\":1}],"
                    + "\"rows\":[\"...\",\".#.\",\"...\"]"
                    + "}"
                    + "}"
            ),
            java.nio.file.Paths.get("inline.json"),
            0
        ).orElseThrow();
        final MapGrid grid = JsonMapgenRunner.run(definition, ctx.palettes, new JsonMapgenRunOptions());

        assertEquals("t_wall", grid.get(1, 1).getTerrainId());
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

    @Test
    void catalogFindsMapgenFromFixture() throws Exception {
        final FixtureContext ctx = loadFixtureContext();
        assertFalse(ctx.catalog.findByOmTerrain("test_room").isEmpty());
        assertTrue(ctx.catalog.runnableOnly().size() >= 2);
    }

    @Test
    void integrationHouse09GroundFloorFromSiblingBn() throws Exception {
        final Path bnData = Paths.get("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(bnData.toFile().isDirectory(), "Cataclysm-BN/data not found beside nextgen");

        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(bnData);
        final PaletteRegistry palettes = PaletteLoader.load(options).getPalettes();
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();

        final List<JsonMapgenDefinition> matches = catalog.findByOmTerrain("house_09");
        assumeTrue(!matches.isEmpty(), "house_09 mapgen missing");

        final JsonMapgenDefinition ground = matches.stream()
            .filter(JsonMapgenDefinition::isJsonPreviewSupported)
            .findFirst()
            .orElse(null);
        assertNotNull(ground);

        final MapGrid grid = JsonMapgenRunner.run(ground, palettes, new JsonMapgenRunOptions());
        assertEquals(24, grid.width());
        assertEquals(24, grid.height());
        assertEquals("t_floor", grid.get(12, 12).getTerrainId());
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
