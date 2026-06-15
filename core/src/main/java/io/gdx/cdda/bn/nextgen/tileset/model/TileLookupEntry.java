package io.gdx.cdda.bn.nextgen.tileset.model;

import io.gdx.cdda.bn.nextgen.tileset.atlas.AtlasPlacement;

/** Cached texture plus UV-warp draw offset (A1). */
public final class TileLookupEntry {

    private final AtlasPlacement placement;
    private final int warpOffsetX;
    private final int warpOffsetY;

    public TileLookupEntry(
        final AtlasPlacement placement,
        final int warpOffsetX,
        final int warpOffsetY
    ) {
        this.placement = placement;
        this.warpOffsetX = warpOffsetX;
        this.warpOffsetY = warpOffsetY;
    }

    public AtlasPlacement getPlacement() {
        return placement;
    }

    public int getWarpOffsetX() {
        return warpOffsetX;
    }

    public int getWarpOffsetY() {
        return warpOffsetY;
    }
}
