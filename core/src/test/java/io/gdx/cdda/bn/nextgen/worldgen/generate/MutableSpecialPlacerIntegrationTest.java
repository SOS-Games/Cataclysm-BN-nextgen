package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MutableSpecialPlacerIntegrationTest {

    private WorldgenPreviewService service;

    @BeforeEach
    void loadWorldgen() throws Exception {
        service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
    }

    @Test
    void generatedOvermapContainsMutableLabFootprint() {
        service.setWorldSeed(900L);
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(900L)
            .withQuotas(1, 0, 1);
        final OvermapGenerateResult result = service.generateOvermap(options);
        assertTrue(result.getMutableSpecialsPlaced() >= 1);

        boolean foundMutableOmt = false;
        final OvermapGrid grid = result.getGrid();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (grid.getOmtId(x, y).startsWith("test_mutable_")) {
                    foundMutableOmt = true;
                }
            }
        }
        assertTrue(foundMutableOmt, "expected assembled mutable special omt ids on grid");
    }
}
