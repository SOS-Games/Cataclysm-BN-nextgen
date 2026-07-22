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
        service.setWorldSeed(4242L);
        final OvermapGenerateResult result = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withRegionId("bn_default_city")
        );

        assertTrue(result.getUrbanOmtsPlaced() > 20, "urban=" + result.getUrbanOmtsPlaced());
        assertTrue(result.getLocalRoadCellsPlaced() > 5, "local=" + result.getLocalRoadCellsPlaced());
        assertTrue(countOmt(result.getGrid(), "test_shop") > 0);
        assertTrue(countOmt(result.getGrid(), "test_park") > 0);
        assertTrue(
            countOmt(result.getGrid(), "test_road_nesw_manhole") > 0,
            "street-first cities should seed a manhole road"
        );
    }

    @Test
    void legacyUrbanFillStillPlacesDenseBlob() {
        service.setWorldSeed(4242L);
        final OvermapGenerateResult streetFirst = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withRegionId("urban_heavy")
        );
        final OvermapGenerateResult legacy = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64)
                .withRegionId("urban_heavy")
                .withLegacyUrbanFill(true)
        );

        assertTrue(
            legacy.getUrbanOmtsPlaced() > streetFirst.getUrbanOmtsPlaced(),
            "legacy=" + legacy.getUrbanOmtsPlaced() + " street=" + streetFirst.getUrbanOmtsPlaced()
        );
        assertTrue(legacy.getLocalRoadCellsPlaced() > 5);
    }

    @Test
    void noCitiesWorldOptionSkipsUrbanFillForBnDefaultCityStyle() {
        service.setWorldSeed(4242L);
        final OvermapGenerateResult withCities = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withRegionId("bn_default_city")
        );
        final OvermapGenerateResult withoutCities = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64)
                .withRegionId("bn_default_city")
                .withWorldOptions(WorldgenWorldOptions.noCities())
        );

        assertTrue(withCities.getUrbanOmtsPlaced() > withoutCities.getUrbanOmtsPlaced());
    }

    @Test
    void urbanHeavyRegionPlacesUrbanOmtsOn64x64() {
        service.setWorldSeed(5150L);
        final OvermapGenerateResult result = service.generateOvermap(
            OvermapGenerateOptions.forSize(64, 64).withRegionId("urban_heavy")
        );

        assertTrue(result.getUrbanOmtsPlaced() > 5, "urban=" + result.getUrbanOmtsPlaced());
        assertTrue(result.getLocalRoadCellsPlaced() > 5, "local=" + result.getLocalRoadCellsPlaced());
        final int shops = countOmt(result.getGrid(), "test_shop");
        final int parks = countOmt(result.getGrid(), "test_park");
        final int houses = countOmt(result.getGrid(), "test_urban_house");
        assertTrue(shops + parks + houses > 5, "shops=" + shops + " parks=" + parks + " houses=" + houses);
        assertTrue(countRoadOmts(result.getGrid()) > result.getRoadCellsPlaced());
    }

    @Test
    void sparseCitiesRegionKeepsLegacyCityPlacerPath() {
        service.setWorldSeed(99L);
        final OvermapGenerateResult urban = service.generateOvermap(
            OvermapGenerateOptions.forSize(32, 32).withRegionId("urban_heavy")
        );
        final OvermapGenerateResult sparse = service.generateOvermap(
            OvermapGenerateOptions.forSize(32, 32).withRegionId("sparse_cities")
        );

        assertTrue(
            urban.getUrbanOmtsPlaced() > sparse.getUrbanOmtsPlaced()
                || urban.getLocalRoadCellsPlaced() > sparse.getLocalRoadCellsPlaced(),
            "urban omts=" + urban.getUrbanOmtsPlaced()
                + " local=" + urban.getLocalRoadCellsPlaced()
                + " sparse omts=" + sparse.getUrbanOmtsPlaced()
                + " local=" + sparse.getLocalRoadCellsPlaced()
        );
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
