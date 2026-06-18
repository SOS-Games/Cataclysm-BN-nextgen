package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;

/** BN-compatible sprite placement for map cells (LibGDX y-up). */
public final class TileDrawMath {

    public static final class DrawRect {
        public final float x;
        public final float y;
        public final float width;
        public final float height;

        public DrawRect(final float x, final float y, final float width, final float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private TileDrawMath() {}

    /**
     * Positions a sprite like BN {@code draw_sprite_at}: anchor at the cell's top-left in screen
     * space, then apply tile offsets. {@code cellBottom} is the LibGDX bottom-left of the cell.
     */
    public static DrawRect computeDrawRect(
        final LoadedTileset tileset,
        final TileDefinition tile,
        final TextureRegion region,
        final float cellLeft,
        final float cellBottom,
        final float tilePx
    ) {
        return computeDrawRect(tileset, tile, region, cellLeft, cellBottom, tilePx, 0);
    }

    public static DrawRect computeDrawRect(
        final LoadedTileset tileset,
        final TileDefinition tile,
        final TextureRegion region,
        final float cellLeft,
        final float cellBottom,
        final float tilePx,
        final int height3dLift
    ) {
        return computeDrawRect(
            tileset.getTileInfo().getWidth(),
            tileset.getTileInfo().getHeight(),
            tile,
            region.getRegionWidth(),
            region.getRegionHeight(),
            cellLeft,
            cellBottom,
            tilePx,
            height3dLift
        );
    }

    public static DrawRect computeDrawRect(
        final int baseTileWidth,
        final int baseTileHeight,
        final TileDefinition tile,
        final int regionWidth,
        final int regionHeight,
        final float cellLeft,
        final float cellBottom,
        final float tilePx,
        final int height3dLift
    ) {
        final float scaleX = tilePx / Math.max(1f, baseTileWidth);
        final float scaleY = tilePx / Math.max(1f, baseTileHeight);
        final float pixelScale = tile.getPixelScale();

        final float width = regionWidth * scaleX * pixelScale;
        final float height = regionHeight * scaleY * pixelScale;

        final float offsetX = tile.getOffsetX() * scaleX;
        final float offsetY = (tile.getOffsetY() - height3dLift) * scaleY;

        final float cellTop = cellBottom + tilePx;
        final float x = cellLeft + offsetX;
        final float y = cellTop - offsetY - height;
        return new DrawRect(x, y, width, height);
    }
}
