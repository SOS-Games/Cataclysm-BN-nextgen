package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.GameDataLoader;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmapGeneratorTest {

    @Test
    void visitGeneratesConnectedPatchForFixtureRoomOmt() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        final io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData gameData = GameDataLoader.loadMods(
            Collections.singletonList("bn"),
            GameDataLoadOptions.fromRoots(Collections.singletonList(WorldgenTestFixtures.fixtureDataRoot()))
        );
        final OvermapGrid overmap = OvermapGridFactory.empty(4, 4, "open_air");
        overmap.setOmtId(1, 1, "test_room");

        final VisitResult result = SubmapGenerator.visit(
            overmap,
            1,
            1,
            0,
            99L,
            new SubmapCache(8),
            service,
            OvermapTerrainLoader.load(
                OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
            ).getRegistry(),
            gameData
        );

        assertFalse(result.isFromCache());
        assertTrue(result.isPatchVisit());
        final int stride = OmtStitchComposer.DEFAULT_OMT_SIZE;
        assertEquals(stride * 3, result.getGrid().width());
        final int origin = stride;
        assertEquals("t_floor", result.getGrid().get(origin + 1, origin + 1).getTerrainId());
        assertEquals("f_chair", result.getGrid().get(origin + 2, origin + 2).getFurnitureId());
    }

    @Test
    void visitReturnsEmptyWhenNoMapgenMatch() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        final OvermapGrid overmap = OvermapGridFactory.empty(2, 2, "bogus_unknown_omt");

        final VisitResult result = SubmapGenerator.visit(
            overmap,
            0,
            0,
            0,
            1L,
            new SubmapCache(8),
            service,
            null,
            null
        );

        assertFalse(result.hasGrid());
    }

    @Test
    void secondVisitSameKeyReturnsCachedPatch() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        final OvermapGrid overmap = OvermapGridFactory.empty(3, 3, "open_air");
        overmap.setOmtId(1, 1, "test_room");
        final SubmapCache cache = new SubmapCache(16);

        final VisitResult first = SubmapGenerator.visit(
            overmap, 1, 1, 0, 42L, cache, service, null, null
        );
        final VisitResult second = SubmapGenerator.visit(
            overmap, 1, 1, 0, 42L, cache, service, null, null
        );

        assertFalse(first.isFromCache());
        assertTrue(second.isFromCache());
        assertEquals(first.getGrid().width(), second.getGrid().width());
    }

    @Test
    void differentWorldSeedsUseDifferentCacheKeys() throws Exception {
        final SubmapKey keyA = new SubmapKey(1L, 2, 3, 0);
        final SubmapKey keyB = new SubmapKey(2L, 2, 3, 0);
        org.junit.jupiter.api.Assertions.assertNotEquals(
            SubmapSeed.mix(1L, keyA),
            SubmapSeed.mix(2L, keyB)
        );
    }
}
