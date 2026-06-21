package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.model.ItemGroupRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupRegistry;

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

class ItemGroupLoaderTest {

    @Test
    void loadCoreBuildsItemGroupRegistryFromFixture() throws Exception {
        final LoadedGameData loaded = GameDataLoader.loadCore(fixtureOptions());
        final ItemGroupRegistry groups = loaded.getItemGroups();

        assertEquals(1, groups.size());
        assertTrue(groups.contains("fixture_loot"));
        assertEquals("fixture_loot", groups.find("fixture_loot").orElseThrow().getDisplayName());
    }

    @Test
    void loadCoreBuildsMonsterGroupRegistryFromFixture() throws Exception {
        final LoadedGameData loaded = GameDataLoader.loadCore(fixtureOptions());
        final MonsterGroupRegistry groups = loaded.getMonsterGroups();

        assertEquals(1, groups.size());
        assertTrue(groups.contains("GROUP_FIXTURE_TEST"));
        assertEquals(
            "mon_fixture_zombie",
            groups.find("GROUP_FIXTURE_TEST").orElseThrow().getDisplayName()
        );
    }

    @Test
    void integrationFindsGroupAntOnSiblingBnData() throws Exception {
        final Path bnData = Paths.get("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(bnData.toFile().isDirectory(), "Cataclysm-BN/data not found beside nextgen");

        final LoadedGameData loaded = GameDataLoader.loadCore(
            GameDataLoadOptions.fromRoots(Collections.singletonList(bnData))
        );

        assertTrue(loaded.getItemGroups().size() > 100);
        assertTrue(loaded.getMonsterGroups().size() > 50);
        assertTrue(loaded.getMonsterGroups().contains("GROUP_ANT"));
        assertEquals("mon_ant", loaded.getMonsterGroups().find("GROUP_ANT").orElseThrow().getDisplayName());
    }

    private static GameDataLoadOptions fixtureOptions() throws URISyntaxException {
        final URL url = ItemGroupLoaderTest.class.getResource("/gamedata-fixtures");
        assertNotNull(url, "fixture gamedata-fixtures missing from test classpath");
        return GameDataLoadOptions.fromRoots(Collections.singletonList(Paths.get(url.toURI())));
    }
}
