package io.gdx.cdda.bn.nextgen.view;

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

class MultitileConnectResolverTest {

    @Test
    void rotationAndSubtileMatchesBnTable() {
        assertEquals(
            new MultitileConnectResolver.ConnectResult(MultitileConnectResolver.Subtile.UNCONNECTED, 0),
            MultitileConnectResolver.rotationAndSubtile(0)
        );
        assertEquals(
            new MultitileConnectResolver.ConnectResult(MultitileConnectResolver.Subtile.CENTER, 0),
            MultitileConnectResolver.rotationAndSubtile(15)
        );
        assertEquals(
            new MultitileConnectResolver.ConnectResult(MultitileConnectResolver.Subtile.END_PIECE, 1),
            MultitileConnectResolver.rotationAndSubtile(2)
        );
        assertEquals(
            new MultitileConnectResolver.ConnectResult(MultitileConnectResolver.Subtile.CORNER, 3),
            MultitileConnectResolver.rotationAndSubtile(5)
        );
        assertEquals(
            new MultitileConnectResolver.ConnectResult(MultitileConnectResolver.Subtile.T_CONNECTION, 2),
            MultitileConnectResolver.rotationAndSubtile(14)
        );
    }

    @Test
    void neighborMaskUsesSouthEastWestNorthBitOrder() {
        final MapGrid grid = new MapGrid(3, 3, "t_grass");
        grid.setTerrain(1, 0, "t_grass");
        grid.setTerrain(2, 1, "t_grass");
        grid.setTerrain(0, 1, "t_grass");
        grid.setTerrain(1, 2, "t_grass");

        assertEquals(15, MultitileConnectResolver.neighborMaskSameId(grid, 1, 1, "t_grass"));
    }

    @Test
    void resolveTerrainDrawPicksCenterOnFullGrassBlock() {
        final LoadedTileset tileset = multitileTileset("t_grass");
        final MapGrid grid = new MapGrid(3, 3, "t_dirt");
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                grid.setTerrain(x, y, "t_grass");
            }
        }

        final MultitileConnectResolver.TerrainDrawResolve center = MultitileConnectResolver.resolveTerrainDraw(
            tileset,
            grid,
            1,
            1,
            "t_grass"
        );
        assertEquals("t_grass_center", center.getTileId());
        assertEquals(0, center.getRotation());
    }

    @Test
    void resolveTerrainDrawPicksCornerOnLShape() {
        final LoadedTileset tileset = multitileTileset("t_grass");
        final MapGrid grid = new MapGrid(3, 3, "t_dirt");
        grid.setTerrain(1, 1, "t_grass");
        grid.setTerrain(2, 1, "t_grass");
        grid.setTerrain(1, 2, "t_grass");

        final MultitileConnectResolver.TerrainDrawResolve corner = MultitileConnectResolver.resolveTerrainDraw(
            tileset,
            grid,
            1,
            1,
            "t_grass"
        );
        assertEquals("t_grass_corner", corner.getTileId());
        assertEquals(0, corner.getRotation());
    }

    @Test
    void resolveTerrainDrawLeavesNonMultitileUnchanged() {
        final LoadedTileset tileset = multitileTileset("t_grass");
        tileset.findTile("t_dirt").orElseThrow();
        final MapGrid grid = new MapGrid(2, 2, "t_dirt");

        final MultitileConnectResolver.TerrainDrawResolve resolve = MultitileConnectResolver.resolveTerrainDraw(
            tileset,
            grid,
            0,
            0,
            "t_dirt"
        );
        assertEquals("t_dirt", resolve.getTileId());
        assertEquals(0, resolve.getRotation());
    }

    @Test
    void resolveTerrainDrawFallsBackToParentWhenSubtileMissing() {
        final LoadedTileset tileset = grassParentOnlyTileset();
        final MapGrid grid = new MapGrid(3, 3, "t_grass");

        final MultitileConnectResolver.TerrainDrawResolve resolve = MultitileConnectResolver.resolveTerrainDraw(
            tileset,
            grid,
            1,
            1,
            "t_grass"
        );
        assertEquals("t_grass", resolve.getTileId());
        assertEquals(0, resolve.getRotation());
    }

    @Test
    void subtileKeyMatchesTileRegistrarOrder() {
        assertEquals("center", MultitileConnectResolver.subtileKey(MultitileConnectResolver.Subtile.CENTER));
        assertEquals("t_connection", MultitileConnectResolver.subtileKey(MultitileConnectResolver.Subtile.T_CONNECTION));
        assertEquals("unconnected", MultitileConnectResolver.subtileKey(MultitileConnectResolver.Subtile.UNCONNECTED));
    }

    private static LoadedTileset multitileTileset(final String parentId) {
        final Map<String, TileDefinition> tiles = new LinkedHashMap<>();
        final TileDefinition parent = new TileDefinition(parentId);
        parent.setMultitile(true);
        tiles.put(parentId, parent);
        for (final MultitileConnectResolver.Subtile subtile : MultitileConnectResolver.Subtile.values()) {
            final String subId = MultitileConnectResolver.subtileTileId(parentId, subtile);
            final TileDefinition definition = new TileDefinition(subId);
            definition.setMultitileSubtile(true);
            definition.setRotates(true);
            tiles.put(subId, definition);
        }
        tiles.put("t_dirt", new TileDefinition("t_dirt"));
        return buildTileset(tiles);
    }

    private static LoadedTileset grassParentOnlyTileset() {
        final Map<String, TileDefinition> tiles = new LinkedHashMap<>();
        final TileDefinition parent = new TileDefinition("t_grass");
        parent.setMultitile(true);
        tiles.put("t_grass", parent);
        return buildTileset(tiles);
    }

    private static LoadedTileset buildTileset(final Map<String, TileDefinition> tiles) {
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
