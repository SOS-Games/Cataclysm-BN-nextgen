package io.gdx.cdda.bn.nextgen.tileset.model;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.tileset.atlas.AtlasPlacement;
import io.gdx.cdda.bn.nextgen.tileset.atlas.ColorPixelFilters;
import io.gdx.cdda.bn.nextgen.tileset.atlas.DynamicAtlas;
import io.gdx.cdda.bn.nextgen.tileset.atlas.DynamicEffectFilters;
import io.gdx.cdda.bn.nextgen.tileset.atlas.PixmapContentHash;
import io.gdx.cdda.bn.nextgen.tileset.atlas.SpriteBlitter;
import io.gdx.cdda.bn.nextgen.tileset.atlas.ColorPixelFilter;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Dynamic-atlas texture cache with lazy effect compositing (A1). */
public final class TileLookup {

    public static final class LookupResult {
        private final TextureRegion region;
        private final int warpOffsetX;
        private final int warpOffsetY;

        public LookupResult(
            final TextureRegion region,
            final int warpOffsetX,
            final int warpOffsetY
        ) {
            this.region = region;
            this.warpOffsetX = warpOffsetX;
            this.warpOffsetY = warpOffsetY;
        }

        public TextureRegion getRegion() {
            return region;
        }

        public int getWarpOffsetX() {
            return warpOffsetX;
        }

        public int getWarpOffsetY() {
            return warpOffsetY;
        }
    }

    private final Map<TileLookupKey, TileLookupEntry> entries = new HashMap<>();

    public void putBase(final int spriteIndex, final AtlasPlacement placement) {
        entries.put(
            TileLookupKey.baseKey(spriteIndex),
            new TileLookupEntry(placement, 0, 0)
        );
    }

    public Optional<TileLookupEntry> findBase(final int spriteIndex) {
        return Optional.ofNullable(entries.get(TileLookupKey.baseKey(spriteIndex)));
    }

    public int size() {
        return entries.size();
    }

    public LookupResult getOrDefault(
        final int spriteIndex,
        final int maskIndex,
        final TilesetFxType effect,
        final TintConfig tint,
        final long warpHash,
        final int spriteOffsetX,
        final int spriteOffsetY,
        final DynamicAtlas atlas,
        final WarpCache warpCache,
        final TilesetLoadOptions options
    ) {
        final TilesetFxType resolvedEffect = effect == null ? TilesetFxType.NONE : effect;
        final TintConfig resolvedTint = tint == null ? TintConfig.NONE : tint;
        if (resolvedEffect == TilesetFxType.NONE
            && maskIndex == TileLookupKey.NO_MASK
            && !resolvedTint.hasTint()
            && warpHash == TileLookupKey.NO_WARP) {
            final TileLookupEntry base = entries.get(TileLookupKey.baseKey(spriteIndex));
            if (base == null) {
                return null;
            }
            return toResult(base);
        }

        final TileLookupKey modKey = new TileLookupKey(
            spriteIndex,
            maskIndex,
            resolvedEffect,
            resolvedTint,
            warpHash,
            spriteOffsetX,
            spriteOffsetY
        );
        final TileLookupEntry cached = entries.get(modKey);
        if (cached != null) {
            return toResult(cached);
        }

        final TileLookupEntry baseEntry = entries.get(TileLookupKey.baseKey(spriteIndex));
        if (baseEntry == null) {
            return null;
        }
        if (warpHash != TileLookupKey.NO_WARP) {
            // Full UV warp compositing is draw-time only; callers without warp support get base+fx.
            final Optional<WarpCache.WarpSurface> warp = warpCache.getWarpSurface(warpHash);
            if (!warp.isPresent()) {
                return toResult(baseEntry);
            }
        }

        final DynamicAtlas.ReadbackSlice baseSlice = atlas.readbackSlice(baseEntry.getPlacement())
            .orElse(null);
        if (baseSlice == null) {
            return toResult(baseEntry);
        }

        final Pixmap source = baseSlice.copyPixels();
        try {
            final ColorPixelFilter filter = DynamicEffectFilters.filterFor(resolvedEffect, options);
            if (filter != null) {
                ColorPixelFilters.applyToPixmap(source, filter);
            }
            final long contentHash = PixmapContentHash.hash(source);
            final AtlasPlacement composite = atlas.getOrCreateSprite(
                source.getWidth(),
                source.getHeight(),
                contentHash,
                blitFrom(source)
            );
            final TileLookupEntry composed = new TileLookupEntry(composite, 0, 0);
            entries.put(modKey, composed);
            return toResult(composed);
        } finally {
            source.dispose();
        }
    }

    private static LookupResult toResult(final TileLookupEntry entry) {
        return new LookupResult(
            entry.getPlacement().toRegion(),
            entry.getWarpOffsetX(),
            entry.getWarpOffsetY()
        );
    }

    private static SpriteBlitter blitFrom(final Pixmap source) {
        return (destination, destX, destY, width, height) ->
            destination.drawPixmap(source, destX, destY, 0, 0, width, height);
    }
}
