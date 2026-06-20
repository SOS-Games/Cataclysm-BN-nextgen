package io.gdx.cdda.bn.nextgen.view;

import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetTextures;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultitileLooksLikeIntegrationTest {

    @Test
    void regionalAliasNeighborsConnectForMultitileGrass() {
        final TerrainRegistry terrains = new TerrainRegistry();
        terrains.put(new TerrainDefinition(
            "t_region_grass",
            "t_region_grass",
            null,
            ".",
            "green",
            2,
            Collections.emptyList(),
            "t_grass",
            "test"
        ));
        terrains.put(new TerrainDefinition(
            "t_grass",
            "t_grass",
            null,
            ".",
            "green",
            2,
            Collections.emptyList(),
            null,
            "test"
        ));

        final LoadedTileset tileset = multitileGrassTileset();
        final MapGrid grid = new MapGrid(3, 3, "t_dirt");
        grid.setTerrain(0, 1, "t_region_grass");
        grid.setTerrain(1, 0, "t_grass");
        grid.setTerrain(1, 1, "t_region_grass");
        grid.setTerrain(2, 1, "t_grass");
        grid.setTerrain(1, 2, "t_grass");

        final MultitileConnectResolver.TerrainDrawResolve center = MultitileConnectResolver.resolveTerrainDraw(
            tileset,
            grid,
            1,
            1,
            "t_region_grass",
            terrains
        );
        assertEquals("t_grass_center", center.getTileId());
    }

    private static LoadedTileset multitileGrassTileset() {
        final Map<String, TileDefinition> tiles = new LinkedHashMap<>();
        final TileDefinition parent = new TileDefinition("t_grass");
        parent.setMultitile(true);
        tiles.put("t_grass", parent);
        for (final MultitileConnectResolver.Subtile subtile : MultitileConnectResolver.Subtile.values()) {
            final String subId = MultitileConnectResolver.subtileTileId("t_grass", subtile);
            tiles.put(subId, new TileDefinition(subId));
        }
        return new LoadedTileset(
            "test",
            new TileInfo(10, 10, 1f, false),
            null,
            TilesetTextures.create(TilesetLoadOptions.defaults(), 10, 10),
            tiles,
            Collections.emptyList(),
            0,
            Collections.emptyList()
        );
    }
}
