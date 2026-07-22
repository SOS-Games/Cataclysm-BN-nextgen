package io.gdx.cdda.bn.nextgen.worldgen;

import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateResult;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;
import io.gdx.cdda.bn.nextgen.worldgen.submap.OmtNeighborhoodStitcher;
import io.gdx.cdda.bn.nextgen.worldgen.submap.VisitResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldgenPreviewServiceTest {

    @Test
    void visitLoadsCatalogAndGeneratesSubmap() throws Exception {
        final WorldgenPreviewService service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        service.setWorldSeed(77L);

        final OvermapGrid overmap = OvermapGridFactory.empty(4, 4, "open_air");
        overmap.setOmtId(2, 2, "test_room");

        final VisitResult first = service.visit(overmap, 2, 2);
        final VisitResult second = service.visit(overmap, 2, 2);

        assertTrue(first.hasGrid());
        assertTrue(first.isPatchVisit());
        assertFalse(first.isFromCache());
        assertTrue(second.isFromCache());
        assertEquals("test_room", first.getOmtId());
        assertEquals(OmtStitchComposer.DEFAULT_OMT_SIZE * 3, first.getGrid().width());
    }

    @Test
    void visitNeighborhoodAdjacentFocusHitsCacheForOverlap() throws Exception {
        final WorldgenPreviewService service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        service.setWorldSeed(99L);
        service.setSubmapCacheCapacity(512);

        final OvermapGrid overmap = OvermapGridFactory.empty(24, 24, "open_air");
        overmap.setOmtId(12, 12, "test_room");
        overmap.setOmtId(13, 12, "test_room");

        final VisitResult first = service.visitNeighborhood(overmap, 12, 12, 0);
        assertTrue(first.hasGrid());
        assertTrue(first.isPatchVisit());
        assertFalse(first.isFromCache());
        assertEquals(
            OmtStitchComposer.DEFAULT_OMT_SIZE * OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_WIDTH,
            first.getGrid().width()
        );
        assertEquals(
            OmtStitchComposer.DEFAULT_OMT_SIZE * OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_HEIGHT,
            first.getGrid().height()
        );

        final VisitResult sameAgain = service.visitNeighborhood(overmap, 12, 12, 0);
        assertTrue(sameAgain.isFromCache());

        final VisitResult adjacent = service.visitNeighborhood(overmap, 13, 12, 0);
        assertTrue(adjacent.hasGrid());
        assertTrue(adjacent.isPatchVisit());
        assertEquals(13, adjacent.getVisitOmtX());
        final VisitResult adjacentAgain = service.visitNeighborhood(overmap, 13, 12, 0);
        assertTrue(adjacentAgain.isFromCache());
        assertEquals(first.getGrid().width(), adjacent.getGrid().width());
    }

    @Test
    void generateOvermapUsesConfiguredRegionId() throws Exception {
        final WorldgenPreviewService service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        service.setWorldSeed(4242L);
        service.setRegionId("urban_heavy");

        final OvermapGenerateResult urban = service.generateOvermap(64, 64);
        service.setRegionId("sparse_cities");
        final OvermapGenerateResult sparse = service.generateOvermap(64, 64);

        assertTrue(urban.getUrbanOmtsPlaced() > sparse.getUrbanOmtsPlaced());
    }
}
