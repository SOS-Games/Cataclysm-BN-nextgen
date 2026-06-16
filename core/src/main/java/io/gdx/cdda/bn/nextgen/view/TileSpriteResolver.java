package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteVariant;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;
import io.gdx.cdda.bn.nextgen.tileset.model.WeightedSpriteList;

/** Shared tile sprite resolution helpers for viewer/editor rendering. */
public final class TileSpriteResolver {

    private TileSpriteResolver() {}

    public static int animationPickIndex(
        final TileDefinition tile,
        final long animationTick,
        final boolean playbackEnabled
    ) {
        if (!playbackEnabled || !tile.isAnimated()) {
            return 0;
        }
        int framesInLoop = tile.getSprites().getForeground().getTotalWeight();
        if (framesInLoop <= 1) {
            framesInLoop = tile.getSprites().getBackground().getTotalWeight();
        }
        if (framesInLoop <= 1) {
            return 0;
        }
        final long seed = tile.getId().hashCode() & 0xffffffffL;
        return (int) Math.floorMod(animationTick + seed, framesInLoop);
    }

    public static TextureRegion resolveBackground(
        final LoadedTileset tileset,
        final TileDefinition tile,
        final int pickIndex,
        final TilesetFxType fxType
    ) {
        return resolveLayer(tileset, tile.getSprites().getBackground(), pickIndex, fxType);
    }

    public static TextureRegion resolveForeground(
        final LoadedTileset tileset,
        final TileDefinition tile,
        final int pickIndex,
        final TilesetFxType fxType
    ) {
        return resolveLayer(tileset, tile.getSprites().getForeground(), pickIndex, fxType);
    }

    private static TextureRegion resolveLayer(
        final LoadedTileset tileset,
        final WeightedSpriteList layer,
        final int pickIndex,
        final TilesetFxType fxType
    ) {
        final int spriteIndex = resolveSpriteIndex(layer, pickIndex);
        return spriteIndex < 0 ? null : tileset.getTexture(spriteIndex, fxType);
    }

    private static int resolveSpriteIndex(final WeightedSpriteList layer, final int pickIndex) {
        if (layer.isEmpty()) {
            return -1;
        }
        final SpriteVariant variant = layer.pickByIndex(pickIndex);
        if (variant == null || variant.isEmpty()) {
            return -1;
        }
        return variant.getFrame(0);
    }

    /** True when the tile id exists in the tileset and has at least one resolved sprite layer. */
    public static boolean hasDrawableArt(final LoadedTileset tileset, final String tileId) {
        if (tileset == null || tileId == null || tileId.isEmpty()) {
            return false;
        }
        return tileset.findTile(tileId)
            .map(tile -> hasDrawableArt(tileset, tile))
            .orElse(false);
    }

    public static boolean hasDrawableArt(final LoadedTileset tileset, final TileDefinition tile) {
        if (tileset == null || tile == null) {
            return false;
        }
        final TextureRegion bg = resolveBackground(tileset, tile, 0, TilesetFxType.NONE);
        final TextureRegion fg = resolveForeground(tileset, tile, 0, TilesetFxType.NONE);
        return bg != null || fg != null;
    }
}
