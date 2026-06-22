package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapGeneratorScaleTest {

    private WorldgenPreviewService service;

    @BeforeEach
    void loadWorldgen() throws Exception {
        service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
    }

    @Test
    void preview64PresetMatchesForSize() {
        final OvermapGenerateOptions preview = OvermapGenerateOptions.preview64();
        final OvermapGenerateOptions sized = OvermapGenerateOptions.forSize(64, 64);
        assertEquals(64, preview.getWidth());
        assertEquals(64, preview.getHeight());
        assertEquals(sized.getCityBuildingQuota(), preview.getCityBuildingQuota());
        assertEquals(sized.getStaticSpecialQuota(), preview.getStaticSpecialQuota());
        assertTrue(preview.getCityBuildingQuota() >= 8);
    }

    @Test
    void bnScaleUsesReducedQuotas() {
        final OvermapGenerateOptions bn = OvermapGenerateOptions.bnScale();
        assertEquals(180, bn.getWidth());
        assertEquals(180, bn.getHeight());
        assertTrue(bn.getCityBuildingQuota() <= 12);
        assertTrue(bn.getStaticSpecialQuota() <= 4);
        assertEquals(1, bn.getMutableSpecialQuota());
    }

    @Test
    void generates64x64GridWithFilledCells() {
        service.setWorldSeed(9001L);
        final OvermapGenerateResult result = service.generateOvermap(64, 64);
        final OvermapGrid grid = result.getGrid();
        assertEquals(64, grid.width());
        assertEquals(64, grid.height());

        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                assertFalse(id == null || id.isEmpty(), "cell (" + x + "," + y + ")");
            }
        }
    }

    @Test
    void submapCacheCapacityIsConfigurable() {
        service.setSubmapCacheCapacity(200);
        assertEquals(200, service.getSubmapCacheCapacity());
    }
}
