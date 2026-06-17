package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameDataLoaderModTest {

    @Test
    void loadModsAppliesLaterModOverride() throws Exception {
        final GameDataLoadOptions options = GameDataLoadOptions.fromRoots(
            Collections.singletonList(ModDiscoveryTest.modFixtureDataRoot())
        );

        final LoadedGameData loaded = GameDataLoader.loadMods(
            Arrays.asList("patch_a", "patch_b"),
            options
        );

        assertEquals("patch b version", loaded.getTerrain().find("t_override_test").orElseThrow().getName());
        assertEquals("patch_b", loaded.getTerrain().find("t_override_test").orElseThrow().getSourceMod());
        assertEquals(Arrays.asList("bn", "patch_a", "patch_b"), loaded.getSourceMods());
    }

    @Test
    void loadModsOrderChangesWhichPatchWins() throws Exception {
        final GameDataLoadOptions options = GameDataLoadOptions.fromRoots(
            Collections.singletonList(ModDiscoveryTest.modFixtureDataRoot())
        );

        final LoadedGameData loaded = GameDataLoader.loadMods(
            Arrays.asList("patch_b", "patch_a"),
            options
        );

        assertEquals("patch a version", loaded.getTerrain().find("t_override_test").orElseThrow().getName());
        assertEquals(Arrays.asList("bn", "patch_b", "patch_a"), loaded.getSourceMods());
    }

    @Test
    void modOrderResolverMovesCoreModToFront() throws Exception {
        final ModRegistry registry = ModDiscovery.discover(
            Collections.singletonList(ModDiscoveryTest.modFixtureDataRoot())
        );

        final List<String> ordered = ModOrderResolver.resolve(
            Arrays.asList("patch_b", "bn", "patch_a"),
            registry
        );

        assertEquals("bn", ordered.get(0));
        assertEquals(Arrays.asList("bn", "patch_b", "patch_a"), ordered);
    }

    @Test
    void modOrderResolverInsertsDefaultCoreWhenAbsent() throws Exception {
        final ModRegistry registry = ModDiscovery.discover(
            Collections.singletonList(ModDiscoveryTest.modFixtureDataRoot())
        );

        final List<String> ordered = ModOrderResolver.resolve(
            Collections.singletonList("patch_a"),
            registry
        );

        assertEquals("bn", ordered.get(0));
        assertEquals(Arrays.asList("bn", "patch_a"), ordered);
    }

    @Test
    void modOrderResolverDeduplicatesSelection() throws Exception {
        final ModRegistry registry = ModDiscovery.discover(
            Collections.singletonList(ModDiscoveryTest.modFixtureDataRoot())
        );

        final List<String> ordered = ModOrderResolver.resolve(
            Arrays.asList("patch_a", "patch_a", "patch_b"),
            registry
        );

        assertEquals(Arrays.asList("bn", "patch_a", "patch_b"), ordered);
    }

    @Test
    void loadCoreUsesDiscoveredBnModFromFixture() throws Exception {
        final LoadedGameData loaded = GameDataLoader.loadCore(
            GameDataLoadOptions.fromRoots(Collections.singletonList(fixtureDataRoot()))
        );

        assertTrue(loaded.getTerrain().size() >= 2);
        assertEquals(Collections.singletonList("bn"), loaded.getSourceMods());
    }

    private static java.nio.file.Path fixtureDataRoot() throws Exception {
        final java.net.URL url = GameDataLoaderModTest.class.getResource("/gamedata-fixtures");
        assertTrue(url != null, "fixture gamedata-fixtures missing from test classpath");
        return java.nio.file.Paths.get(url.toURI());
    }
}
