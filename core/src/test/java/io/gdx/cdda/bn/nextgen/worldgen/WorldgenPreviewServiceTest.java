package io.gdx.cdda.bn.nextgen.worldgen;

import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateResult;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;
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
