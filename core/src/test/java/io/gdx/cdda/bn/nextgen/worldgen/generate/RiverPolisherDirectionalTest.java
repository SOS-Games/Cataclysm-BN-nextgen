package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiverPolisherDirectionalTest {

    @Test
    void rewritesVerticalRiverSegmentToDirectionalId() throws Exception {
        final OvermapGrid grid = new OvermapGrid(8, 8, "test_field");
        grid.setOmtId(4, 1, "test_river");
        grid.setOmtId(4, 2, "test_river");
        grid.setOmtId(4, 3, "test_river");
        final OvermapGenerateOptions options = OvermapGenerateOptions.forSize(8, 8)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, false, "local_road", "test_river", "test_river");
        final OvermapTerrainRegistry registry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(
                io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures.fixtureDataRoot()
            )
        ).getRegistry();

        final int polished = RiverPolisher.polishDirectional(grid, options, registry, new ArrayList<>());

        assertTrue(polished >= 1);
        assertEquals("test_river_north", grid.getOmtId(4, 2));
    }
}
