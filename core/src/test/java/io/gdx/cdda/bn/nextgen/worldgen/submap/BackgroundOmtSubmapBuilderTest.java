package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundOmtSubmapBuilderTest {

    @Test
    void buildsGrassField() {
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "field",
            1L,
            null,
            null
        );
        assertTrue(grid.isPresent());
        assertEquals("t_grass", grid.get().get(0, 0).getTerrainId());
    }

    @Test
    void buildsForestWithTrees() {
        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            null,
            0,
            0,
            "forest",
            99L,
            null,
            null
        );
        assertTrue(grid.isPresent());
        boolean sawTree = false;
        for (int y = 0; y < grid.get().height(); y++) {
            for (int x = 0; x < grid.get().width(); x++) {
                final String ter = grid.get().get(x, y).getTerrainId();
                if ("t_tree".equals(ter) || "t_tree_young".equals(ter)) {
                    sawTree = true;
                }
            }
        }
        assertTrue(sawTree);
    }

    @Test
    void buildsConnectedRoadPatch() throws Exception {
        final OvermapConnectionRegistry connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final OvermapGrid overmap = OvermapGridFactory.empty(3, 3, "test_field");
        overmap.setOmtId(1, 0, "test_road_ns");
        overmap.setOmtId(1, 1, "test_road_ns");
        overmap.setOmtId(1, 2, "test_road_ns");

        final Optional<MapGrid> grid = BackgroundOmtSubmapBuilder.buildIfSupported(
            overmap,
            1,
            1,
            "test_road_ns",
            5L,
            connections,
            null
        );
        assertTrue(grid.isPresent());
        assertEquals("t_pavement", grid.get().get(12, 12).getTerrainId());
        assertEquals("t_grass", grid.get().get(0, 0).getTerrainId());
    }
}
