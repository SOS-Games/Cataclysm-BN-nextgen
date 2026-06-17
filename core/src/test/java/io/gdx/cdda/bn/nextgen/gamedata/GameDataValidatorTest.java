package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.validate.GameDataValidationException;
import io.gdx.cdda.bn.nextgen.gamedata.validate.GameDataValidator;
import io.gdx.cdda.bn.nextgen.gamedata.validate.ValidationOptions;
import io.gdx.cdda.bn.nextgen.gamedata.validate.ValidationReport;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GameDataValidatorTest {

    @Test
    void emptyTerrainRegistryProducesV1Error() throws GameDataValidationException {
        final LoadedGameData data = new LoadedGameData(
            new TerrainRegistry(),
            new FurnitureRegistry(),
            Collections.emptyList()
        );

        final ValidationReport report = GameDataValidator.validate(data, ValidationOptions.defaults());

        assertEquals(1, report.getErrors().size());
        assertTrue(report.getErrors().get(0).contains("[V1]"));
    }

    @Test
    void failOnErrorThrowsForEmptyTerrain() {
        final LoadedGameData data = new LoadedGameData(
            new TerrainRegistry(),
            new FurnitureRegistry(),
            Collections.emptyList()
        );

        final GameDataValidationException ex = assertThrows(
            GameDataValidationException.class,
            () -> GameDataValidator.validate(data, ValidationOptions.defaults().withFailOnError(true))
        );
        assertTrue(ex.getReport().getErrors().get(0).contains("[V1]"));
    }

    @Test
    void warnsOnMissingLooksLikeTarget() throws GameDataValidationException {
        final TerrainRegistry terrain = new TerrainRegistry();
        terrain.put(new TerrainDefinition(
            "t_bad",
            "bad terrain",
            null,
            ".",
            "brown",
            2,
            Collections.emptyList(),
            "t_nonexistent",
            "core"
        ));
        final LoadedGameData data = new LoadedGameData(
            terrain,
            new FurnitureRegistry(),
            Collections.singletonList("core")
        );

        final ValidationReport report = GameDataValidator.validate(data, ValidationOptions.defaults());

        assertFalse(report.hasErrors());
        assertEquals(1, report.getWarnings().size());
        final String warning = report.getWarnings().get(0);
        assertTrue(warning.contains("[V3]"));
        assertTrue(warning.contains("t_bad"));
        assertTrue(warning.contains("t_nonexistent"));
    }

    @Test
    void validLooksLikeTargetInEitherRegistryPasses() throws GameDataValidationException {
        final TerrainRegistry terrain = new TerrainRegistry();
        terrain.put(new TerrainDefinition(
            "t_floor",
            "floor",
            null,
            ".",
            "brown",
            2,
            Collections.emptyList(),
            null,
            "core"
        ));
        final FurnitureRegistry furniture = new FurnitureRegistry();
        furniture.put(new FurnitureDefinition(
            "f_chair",
            "chair",
            "#",
            "brown",
            0,
            0,
            Collections.emptyList(),
            "t_floor",
            "core"
        ));
        final LoadedGameData data = new LoadedGameData(
            terrain,
            furniture,
            Collections.singletonList("core")
        );

        final ValidationReport report = GameDataValidator.validate(data, ValidationOptions.defaults());

        assertFalse(report.hasErrors());
        assertTrue(report.getWarnings().isEmpty());
    }

    @Test
    void gfxCrossCheckWarnsMissingTerrainTiles() throws GameDataValidationException {
        final TerrainRegistry terrain = new TerrainRegistry();
        terrain.put(sampleTerrain("t_has_gfx"));
        terrain.put(sampleTerrain("t_missing_gfx"));
        final LoadedGameData data = new LoadedGameData(
            terrain,
            new FurnitureRegistry(),
            Collections.singletonList("core")
        );
        final LoadedTileset tileset = minimalTileset("t_has_gfx", "unknown");

        final ValidationReport report = GameDataValidator.validate(
            data,
            ValidationOptions.withTileset(tileset)
        );

        assertTrue(report.getWarnings().stream()
            .anyMatch(w -> w.contains("[V5]") && w.contains("t_missing_gfx")));
        assertFalse(report.getWarnings().stream()
            .anyMatch(w -> w.contains("t_has_gfx")));
    }

    @Test
    void emptyDataRootProducesV1ErrorAfterLoad(@TempDir final Path emptyRoot) throws Exception {
        Files.createDirectories(emptyRoot.resolve("json/furniture_and_terrain"));
        final LoadedGameData loaded = GameDataLoader.loadCore(
            GameDataLoadOptions.fromRoots(Collections.singletonList(emptyRoot))
        );

        final ValidationReport report = GameDataValidator.validate(loaded, ValidationOptions.defaults());

        assertTrue(report.getErrors().stream().anyMatch(e -> e.contains("[V1]")));
    }

    @Test
    void fixtureLoadPassesV1() throws Exception {
        final LoadedGameData loaded = GameDataLoader.loadCore(
            GameDataLoadOptions.fromRoots(Collections.singletonList(fixtureDataRoot()))
        );

        final ValidationReport report = GameDataValidator.validate(loaded, ValidationOptions.defaults());

        assertFalse(report.hasErrors());
    }

    @Test
    void validationCompletesQuicklyOnFullBnData() throws Exception {
        final Path bnData = Path.of("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(bnData.toFile().isDirectory(), "Cataclysm-BN/data not found beside nextgen");

        final LoadedGameData loaded = GameDataLoader.loadCore(
            GameDataLoadOptions.fromRoots(Collections.singletonList(bnData))
        );

        final long startNanos = System.nanoTime();
        final ValidationReport report = GameDataValidator.validate(loaded, ValidationOptions.defaults());
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        assertFalse(report.hasErrors());
        assertTrue(elapsedMs < 1000L, "validation took " + elapsedMs + "ms");
    }

    private static TerrainDefinition sampleTerrain(final String id) {
        return new TerrainDefinition(
            id,
            id,
            null,
            ".",
            "brown",
            2,
            Collections.emptyList(),
            null,
            "core"
        );
    }

    private static Path fixtureDataRoot() throws URISyntaxException {
        final URL url = GameDataValidatorTest.class.getResource("/gamedata-fixtures");
        if (url == null) {
            throw new IllegalStateException("fixture gamedata-fixtures missing from test classpath");
        }
        return Paths.get(url.toURI());
    }

    private static LoadedTileset minimalTileset(final String... tileIds) {
        final Map<String, TileDefinition> tiles = new LinkedHashMap<>();
        for (final String tileId : tileIds) {
            tiles.put(tileId, new TileDefinition(tileId));
        }
        return new LoadedTileset(
            "test-tileset",
            new TileInfo(10, 10, 1f, false),
            null,
            io.gdx.cdda.bn.nextgen.tileset.model.TilesetTextures.create(
                TilesetLoadOptions.defaults(),
                10,
                10
            ),
            tiles,
            Collections.emptyList(),
            0,
            Collections.emptyList()
        );
    }
}
