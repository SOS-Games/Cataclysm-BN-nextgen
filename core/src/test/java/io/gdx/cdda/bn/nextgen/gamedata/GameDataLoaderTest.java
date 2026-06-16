package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GameDataLoaderTest {

    @Test
    void loadCoreBuildsTerrainRegistryFromFixture() throws Exception {
        final Path fixtureDataRoot = fixtureDataRoot();
        final GameDataLoadOptions options = GameDataLoadOptions.fromRoots(
            Collections.singletonList(fixtureDataRoot)
        );

        final LoadedGameData loaded = GameDataLoader.loadCore(options);
        final TerrainRegistry terrain = loaded.getTerrain();
        final FurnitureRegistry furniture = loaded.getFurniture();

        assertEquals(2, terrain.size());
        assertTrue(terrain.contains("t_fixture_floor"));
        assertTrue(terrain.contains("t_fixture_single"));
        assertEquals("fixture floor", terrain.find("t_fixture_floor").orElseThrow().getName());
        assertEquals(1, furniture.size());
        assertTrue(furniture.contains("f_fixture_chair"));
        assertEquals("fixture chair", furniture.find("f_fixture_chair").orElseThrow().getName());
    }

    @Test
    void integrationLoadCoreFindsTDirtWhenDataRootPropertySet() throws Exception {
        final String rootProperty = System.getProperty(DataPaths.DATA_ROOTS_PROPERTY);
        assumeTrue(rootProperty != null && !rootProperty.isEmpty(), "Set -Dcdda.data.roots=... to run");

        final LoadedGameData loaded = GameDataLoader.loadCore(GameDataLoadOptions.defaults());
        final TerrainRegistry terrain = loaded.getTerrain();

        assumeTrue(terrain.size() > 0, "No terrain loaded from data roots");
        assumeTrue(terrain.find("t_dirt").isPresent(), "t_dirt missing in current BN data root");
        assertEquals("dirt", terrain.find("t_dirt").orElseThrow().getName());
    }

    @Test
    void integrationCoreTerrainCountIsLargeWhenSiblingBnDataPresent() throws Exception {
        final Path bnData = Paths.get("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(bnData.toFile().isDirectory(), "Cataclysm-BN/data not found beside nextgen");

        final GameDataLoadOptions options = GameDataLoadOptions.fromRoots(
            Collections.singletonList(bnData)
        );
        final LoadedGameData loaded = GameDataLoader.loadCore(options);

        assertTrue(loaded.getTerrain().size() > 500);
        assertTrue(loaded.getFurniture().size() > 200);
    }

    private static Path fixtureDataRoot() throws URISyntaxException {
        final URL url = GameDataLoaderTest.class.getResource("/gamedata-fixtures");
        assertNotNull(url, "fixture gamedata-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }
}
