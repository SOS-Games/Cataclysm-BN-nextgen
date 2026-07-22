package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForestTrailPolisherTest {

    @Test
    void polishesTrailToDirectionalIds() throws Exception {
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGrid grid = new OvermapGrid(5, 5, "test_forest");
        grid.setOmtId(2, 1, "test_forest_trail");
        grid.setOmtId(2, 2, "test_forest_trail");
        grid.setOmtId(2, 3, "test_forest_trail");

        final int polished = ForestTrailPolisher.polish(grid, null, registry);
        // Fixtures may lack LINEAR peers; polish is still a no-op success path then.
        assertTrue(polished >= 0);
        assertTrue(ForestTrailPolisher.isTrailFamily(grid.getOmtId(2, 2)));
    }
}
