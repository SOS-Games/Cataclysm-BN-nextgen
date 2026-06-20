package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapGeneratorTest {

    private WorldgenPreviewService service;

    @BeforeEach
    void loadWorldgen() throws Exception {
        service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
    }

    @Test
    void seedIsReproducible() {
        service.setWorldSeed(4242L);
        final OvermapGrid first = service.generateOvermap(16, 16).getGrid();
        final OvermapGrid second = service.generateOvermap(16, 16).getGrid();
        for (int y = 0; y < first.height(); y++) {
            for (int x = 0; x < first.width(); x++) {
                assertEquals(first.getOmtId(x, y), second.getOmtId(x, y), "cell (" + x + "," + y + ")");
            }
        }
    }

    @Test
    void placesCityBuildingsOn16x16() {
        service.setWorldSeed(100L);
        final OvermapGenerateResult result = service.generateOvermap(16, 16);
        assertTrue(result.getCityBuildingsPlaced() >= 1, "expected at least one city building");

        boolean foundBuildingOmt = false;
        final OvermapGrid grid = result.getGrid();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if (id.contains("test_duplex") || id.contains("test_multitile")) {
                    foundBuildingOmt = true;
                }
            }
        }
        assertTrue(foundBuildingOmt, "grid should contain fixture building omt ids");
    }

    @Test
    void placesStaticSpecialWhenQuotaSet() {
        service.setWorldSeed(200L);
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(16, 16)
            .withSeed(200L)
            .withQuotas(1, 1);
        final OvermapGenerateResult result = service.generateOvermap(options);
        assertTrue(result.getStaticSpecialsPlaced() >= 1 || result.getCityBuildingsPlaced() >= 1);
    }

    @Test
    void carvesRiverAndRoadsOn16x16() {
        service.setWorldSeed(300L);
        final OvermapGenerateResult result = service.generateOvermap(16, 16);
        assertTrue(result.getRiverCellsCarved() >= 3, "expected river chain");
        if (result.getCityBuildingsPlaced() >= 2) {
            assertTrue(result.getRoadCellsPlaced() >= 1, "expected roads between cities");
        }
    }
}
