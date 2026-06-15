package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.Objects;

/** Cache key for composed dynamic-atlas textures (A1 / BN {@code tileset_lookup_key}). */
public final class TileLookupKey {

    public static final int NO_MASK = -1;
    public static final long NO_WARP = 0L;

    private final int spriteIndex;
    private final int maskIndex;
    private final TilesetFxType effect;
    private final TintConfig tint;
    private final long warpHash;
    private final int spriteOffsetX;
    private final int spriteOffsetY;

    public TileLookupKey(
        final int spriteIndex,
        final int maskIndex,
        final TilesetFxType effect,
        final TintConfig tint,
        final long warpHash,
        final int spriteOffsetX,
        final int spriteOffsetY
    ) {
        this.spriteIndex = spriteIndex;
        this.maskIndex = maskIndex;
        this.effect = effect;
        this.tint = tint;
        this.warpHash = warpHash;
        this.spriteOffsetX = spriteOffsetX;
        this.spriteOffsetY = spriteOffsetY;
    }

    public static TileLookupKey baseKey(final int spriteIndex) {
        return new TileLookupKey(
            spriteIndex,
            NO_MASK,
            TilesetFxType.NONE,
            TintConfig.NONE,
            NO_WARP,
            0,
            0
        );
    }

    public int getSpriteIndex() {
        return spriteIndex;
    }

    public int getMaskIndex() {
        return maskIndex;
    }

    public TilesetFxType getEffect() {
        return effect;
    }

    public TintConfig getTint() {
        return tint;
    }

    public long getWarpHash() {
        return warpHash;
    }

    public int getSpriteOffsetX() {
        return spriteOffsetX;
    }

    public int getSpriteOffsetY() {
        return spriteOffsetY;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TileLookupKey)) {
            return false;
        }
        final TileLookupKey other = (TileLookupKey) obj;
        return spriteIndex == other.spriteIndex
            && maskIndex == other.maskIndex
            && effect == other.effect
            && tint.equals(other.tint)
            && warpHash == other.warpHash
            && spriteOffsetX == other.spriteOffsetX
            && spriteOffsetY == other.spriteOffsetY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(spriteIndex, maskIndex, effect, tint, warpHash, spriteOffsetX, spriteOffsetY);
    }
}
