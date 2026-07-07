package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapGeneratorOrderTest {

    private WorldgenPreviewService service;

    @BeforeEach
    void loadWorldgen() throws Exception {
        service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
    }

    @Test
    void urbanHeavyPlacesInterCityHighways() {
        final OvermapGenerateResult result = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withSeed(5150L).withRegionId("urban_heavy")
        );

        assertTrue(result.getRoadCellsPlaced() >= 3, "expected inter-city highway cells");
        assertTrue(
            result.getRoadCellsPlaced() < result.getGrid().width() * result.getGrid().height() / 8,
            "wilderness should not be fully paved"
        );
    }

    @Test
    void highwayRoadCountIsInsensitiveToStaticSpecialQuota() {
        final long seed = 909L;
        final OvermapGenerateResult lowSpecialQuota = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64)
                .withSeed(seed)
                .withRegionId("urban_heavy")
                .withQuotas(12, 0)
        );
        final OvermapGenerateResult highSpecialQuota = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64)
                .withSeed(seed)
                .withRegionId("urban_heavy")
                .withQuotas(12, 8)
        );

        assertTrue(lowSpecialQuota.getRoadCellsPlaced() >= 3);
        assertTrue(highSpecialQuota.getStaticSpecialsPlaced() > lowSpecialQuota.getStaticSpecialsPlaced());
        assertEquals(
            lowSpecialQuota.getRoadCellsPlaced(),
            highSpecialQuota.getRoadCellsPlaced(),
            "highways should connect cities only, not every special site"
        );
    }
}
