package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UrbanOmtPlacerTest {

    private OvermapGrid grid;
    private RegionSettingsDefinition urbanRegion;
    private io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry oterRegistry;

    @BeforeEach
    void setUp() throws Exception {
        grid = new OvermapGrid(24, 24, "open_air");
        BaseTerrainFiller.fill(
            grid,
            OvermapGenerateOptions.forSize(24, 24),
            null,
            new Random(1L)
        );
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        urbanRegion = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("urban_heavy").orElseThrow();
    }

    @Test
    void fillBlobPlacesShopAndParkOmts() {
        final UrbanSite site = new UrbanSite(12, 12, 4, CityTier.LARGE);
        final int placed = UrbanOmtPlacer.fillBlob(
            grid,
            site,
            urbanRegion.getCityContentWeights(),
            OvermapGenerateOptions.forSize(24, 24),
            oterRegistry,
            new Random(42L),
            new ArrayList<>()
        );

        assertTrue(placed > 8);
        assertTrue(countOmt("test_shop") > 0);
        assertTrue(countOmt("test_park") > 0);
        assertTrue(countOmt("test_urban_house") > 0);
    }

    private int countOmt(final String omtId) {
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
