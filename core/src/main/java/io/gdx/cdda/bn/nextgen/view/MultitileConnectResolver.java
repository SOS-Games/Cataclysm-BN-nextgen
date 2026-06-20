package io.gdx.cdda.bn.nextgen.view;

import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Same-id cardinal neighbor mask → multitile subtile id (R1). */
public final class MultitileConnectResolver {

    private static final List<String> SUBTILE_KEYS = Collections.unmodifiableList(
        Arrays.asList(
            "center",
            "corner",
            "edge",
            "t_connection",
            "end_piece",
            "unconnected",
            "open",
            "broken"
        )
    );

    public enum Subtile {
        CENTER,
        CORNER,
        EDGE,
        T_CONNECTION,
        END_PIECE,
        UNCONNECTED,
        OPEN,
        BROKEN
    }

    public static final class ConnectResult {
        private final Subtile subtile;
        private final int rotation;

        public ConnectResult(final Subtile subtile, final int rotation) {
            this.subtile = subtile;
            this.rotation = rotation;
        }

        public Subtile getSubtile() {
            return subtile;
        }

        public int getRotation() {
            return rotation;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ConnectResult)) {
                return false;
            }
            final ConnectResult that = (ConnectResult) other;
            return rotation == that.rotation && subtile == that.subtile;
        }

        @Override
        public int hashCode() {
            return subtile.hashCode() * 31 + rotation;
        }
    }

    public static final class TerrainDrawResolve {
        private final String tileId;
        private final int rotation;

        public TerrainDrawResolve(final String tileId, final int rotation) {
            this.tileId = tileId;
            this.rotation = rotation;
        }

        public String getTileId() {
            return tileId;
        }

        public int getRotation() {
            return rotation;
        }
    }

    private MultitileConnectResolver() {}

    public static int neighborMaskSameId(
        final MapGrid grid,
        final int x,
        final int y,
        final String terrainId
    ) {
        if (grid == null || terrainId == null || terrainId.isEmpty()) {
            return 0;
        }
        int mask = 0;
        if (matchesTerrain(grid, x, y + 1, terrainId)) {
            mask |= 1;
        }
        if (matchesTerrain(grid, x + 1, y, terrainId)) {
            mask |= 2;
        }
        if (matchesTerrain(grid, x - 1, y, terrainId)) {
            mask |= 4;
        }
        if (matchesTerrain(grid, x, y - 1, terrainId)) {
            mask |= 8;
        }
        return mask;
    }

    public static ConnectResult rotationAndSubtile(final int mask) {
        switch (mask) {
            case 0:
                return new ConnectResult(Subtile.UNCONNECTED, 0);
            case 15:
                return new ConnectResult(Subtile.CENTER, 0);
            case 1:
                return new ConnectResult(Subtile.END_PIECE, 0);
            case 2:
                return new ConnectResult(Subtile.END_PIECE, 1);
            case 4:
                return new ConnectResult(Subtile.END_PIECE, 3);
            case 8:
                return new ConnectResult(Subtile.END_PIECE, 2);
            case 9:
                return new ConnectResult(Subtile.EDGE, 0);
            case 6:
                return new ConnectResult(Subtile.EDGE, 1);
            case 3:
                return new ConnectResult(Subtile.CORNER, 0);
            case 10:
                return new ConnectResult(Subtile.CORNER, 1);
            case 12:
                return new ConnectResult(Subtile.CORNER, 2);
            case 5:
                return new ConnectResult(Subtile.CORNER, 3);
            case 7:
                return new ConnectResult(Subtile.T_CONNECTION, 0);
            case 11:
                return new ConnectResult(Subtile.T_CONNECTION, 1);
            case 14:
                return new ConnectResult(Subtile.T_CONNECTION, 2);
            case 13:
                return new ConnectResult(Subtile.T_CONNECTION, 3);
            default:
                return new ConnectResult(Subtile.UNCONNECTED, 0);
        }
    }

    public static String subtileKey(final Subtile subtile) {
        return SUBTILE_KEYS.get(subtile.ordinal());
    }

    public static String subtileTileId(final String terrainId, final Subtile subtile) {
        return terrainId + "_" + subtileKey(subtile);
    }

    public static TerrainDrawResolve resolveTerrainDraw(
        final LoadedTileset tileset,
        final MapGrid grid,
        final int x,
        final int y,
        final String terrainId
    ) {
        return resolveTerrainDraw(tileset, grid, x, y, terrainId, null);
    }

    public static TerrainDrawResolve resolveTerrainDraw(
        final LoadedTileset tileset,
        final MapGrid grid,
        final int x,
        final int y,
        final String terrainId,
        final TerrainRegistry terrains
    ) {
        if (terrainId == null || terrainId.isEmpty()) {
            return new TerrainDrawResolve(terrainId, 0);
        }
        if (tileset == null || grid == null) {
            return new TerrainDrawResolve(terrainId, 0);
        }

        final String connectId = TileLooksLikeResolver.resolveTerrainConnectId(terrainId, terrains);
        final Optional<TileDefinition> parent = tileset.findTile(connectId);
        if (!parent.isPresent() || !parent.get().isMultitile()) {
            return new TerrainDrawResolve(terrainId, 0);
        }

        final int mask = neighborMaskSameId(grid, x, y, terrainId, terrains);
        final ConnectResult connect = rotationAndSubtile(mask);
        final String subId = subtileTileId(connectId, connect.getSubtile());
        if (tileset.findTile(subId).isPresent()) {
            return new TerrainDrawResolve(subId, connect.getRotation());
        }
        return new TerrainDrawResolve(connectId, connect.getRotation());
    }

    private static int neighborMaskSameId(
        final MapGrid grid,
        final int x,
        final int y,
        final String terrainId,
        final TerrainRegistry terrains
    ) {
        if (grid == null || terrainId == null || terrainId.isEmpty()) {
            return 0;
        }
        final String centerConnectId = TileLooksLikeResolver.resolveTerrainConnectId(terrainId, terrains);
        int mask = 0;
        if (matchesConnectTerrain(grid, x, y + 1, centerConnectId, terrains)) {
            mask |= 1;
        }
        if (matchesConnectTerrain(grid, x + 1, y, centerConnectId, terrains)) {
            mask |= 2;
        }
        if (matchesConnectTerrain(grid, x - 1, y, centerConnectId, terrains)) {
            mask |= 4;
        }
        if (matchesConnectTerrain(grid, x, y - 1, centerConnectId, terrains)) {
            mask |= 8;
        }
        return mask;
    }

    private static boolean matchesConnectTerrain(
        final MapGrid grid,
        final int x,
        final int y,
        final String connectId,
        final TerrainRegistry terrains
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        final String neighborId = grid.get(x, y).getTerrainId();
        if (connectId.equals(neighborId)) {
            return true;
        }
        if (terrains == null) {
            return false;
        }
        return connectId.equals(TileLooksLikeResolver.resolveTerrainConnectId(neighborId, terrains));
    }

    private static boolean matchesTerrain(
        final MapGrid grid,
        final int x,
        final int y,
        final String terrainId
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return terrainId.equals(grid.get(x, y).getTerrainId());
    }
}
