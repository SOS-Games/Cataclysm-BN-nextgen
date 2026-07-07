package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenWorldOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CityGeneratorTest {

    private WorldgenPreviewService service;

    @BeforeEach
    void loadWorldgen() throws Exception {
        service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
    }

    @Test
    void bnDefaultCityStyleRegionUsesWorldOptionsForUrbanFill() {
        final OvermapGenerateResult result = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withSeed(4242L).withRegionId("bn_default_city")
        );

        assertTrue(result.getUrbanOmtsPlaced() > 20);
        assertTrue(result.getLocalRoadCellsPlaced() > 5);
        assertTrue(countOmt(result.getGrid(), "test_shop") > 0);
        assertTrue(countOmt(result.getGrid(), "test_park") > 0);
    }

    @Test
    void noCitiesWorldOptionSkipsUrbanFillForBnDefaultCityStyle() {
        final OvermapGenerateResult withCities = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withSeed(4242L).withRegionId("bn_default_city")
        );
        final OvermapGenerateResult withoutCities = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64)
                .withSeed(4242L)
                .withRegionId("bn_default_city")
                .withWorldOptions(WorldgenWorldOptions.noCities())
        );

        assertTrue(withCities.getUrbanOmtsPlaced() > withoutCities.getUrbanOmtsPlaced());
    }

    @Test
    void urbanHeavyRegionPlacesUrbanOmtsOn64x64() {
        final OvermapGenerateResult result = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withSeed(5150L).withRegionId("urban_heavy")
        );

        assertTrue(result.getUrbanOmtsPlaced() > 20);
        assertTrue(result.getLocalRoadCellsPlaced() > 5);
        assertTrue(countOmt(result.getGrid(), "test_shop") > 0);
        assertTrue(countOmt(result.getGrid(), "test_park") > 0);
        assertTrue(countOmt(result.getGrid(), "test_urban_house") > 0);
        assertTrue(countRoadOmts(result.getGrid()) > result.getRoadCellsPlaced());
    }

    @Test
    void sparseCitiesRegionKeepsLegacyCityPlacerPath() {
        final OvermapGenerateResult urban = service.generateOvermap(
            OvermapGenerateOptions.forSize(32, 32).withSeed(99L).withRegionId("urban_heavy")
        );
        final OvermapGenerateResult sparse = service.generateOvermap(
            OvermapGenerateOptions.forSize(32, 32).withSeed(99L).withRegionId("sparse_cities")
        );

        assertTrue(urban.getUrbanOmtsPlaced() > sparse.getUrbanOmtsPlaced());
        assertTrue(urban.getLocalRoadCellsPlaced() > sparse.getLocalRoadCellsPlaced());
    }

    private static int countRoadOmts(final OvermapGrid grid) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                if (id != null && id.startsWith("test_road")) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countOmt(final OvermapGrid grid, final String omtId) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (omtId.equals(grid.getOmtId(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }
}
