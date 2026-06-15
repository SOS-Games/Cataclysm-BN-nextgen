package io.gdx.cdda.bn.nextgen.tileset.model;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.tileset.atlas.AtlasPlacement;
import io.gdx.cdda.bn.nextgen.tileset.atlas.DynamicAtlas;
import io.gdx.cdda.bn.nextgen.tileset.atlas.SpriteBlitter;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadMode;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;

import java.util.Optional;

/** Unified sprite storage for baked-table or dynamic-atlas modes (A1). */
public final class TilesetTextures {

    private final TilesetLoadMode mode;
    private final SpriteTextureTables bakedTables;
    private final DynamicAtlas dynamicAtlas;
    private final TileLookup tileLookup;
    private final WarpCache warpCache;
    private final TilesetLoadOptions options;

    private TilesetTextures(
        final TilesetLoadMode mode,
        final SpriteTextureTables bakedTables,
        final DynamicAtlas dynamicAtlas,
        final TileLookup tileLookup,
        final WarpCache warpCache,
        final TilesetLoadOptions options
    ) {
        this.mode = mode;
        this.bakedTables = bakedTables;
        this.dynamicAtlas = dynamicAtlas;
        this.tileLookup = tileLookup;
        this.warpCache = warpCache;
        this.options = options;
    }

    public static TilesetTextures create(
        final TilesetLoadOptions loadOptions,
        final int tileWidth,
        final int tileHeight
    ) {
        if (loadOptions.getLoadMode() == TilesetLoadMode.DYNAMIC_ATLAS) {
            final int maxSize = loadOptions.resolveMaxTextureWidth(tileWidth);
            return new TilesetTextures(
                TilesetLoadMode.DYNAMIC_ATLAS,
                null,
                new DynamicAtlas(maxSize, maxSize, tileWidth, tileHeight),
                new TileLookup(),
                new WarpCache(),
                loadOptions
            );
        }
        return new TilesetTextures(
            TilesetLoadMode.BAKED_TABLES,
            new SpriteTextureTables(),
            null,
            null,
            null,
            loadOptions
        );
    }

    public TilesetLoadMode getMode() {
        return mode;
    }

    public boolean isDynamicAtlas() {
        return mode == TilesetLoadMode.DYNAMIC_ATLAS;
    }

    public SpriteTextureTables getBakedTables() {
        return bakedTables;
    }

    public DynamicAtlas getDynamicAtlas() {
        return dynamicAtlas;
    }

    public TileLookup getTileLookup() {
        return tileLookup;
    }

    public WarpCache getWarpCache() {
        return warpCache;
    }

    public void beginLoad() {
        if (isDynamicAtlas()) {
            dynamicAtlas.startBatch();
        }
    }

    public void finishLoad() {
        if (isDynamicAtlas()) {
            dynamicAtlas.endBatch();
            dynamicAtlas.readbackLoad();
        }
    }

    public void ensureCapacity(final int requiredSize) {
        if (bakedTables != null) {
            bakedTables.ensureCapacity(requiredSize);
        }
    }

    public int size() {
        if (bakedTables != null) {
            return bakedTables.size();
        }
        return 0;
    }

    public TextureRegion getRegion(final int spriteIndex, final TilesetFxType fxType) {
        if (bakedTables != null) {
            return bakedTables.getRegion(spriteIndex, fxType);
        }
        final TileLookup.LookupResult result = tileLookup.getOrDefault(
            spriteIndex,
            TileLookupKey.NO_MASK,
            fxType,
            TintConfig.NONE,
            TileLookupKey.NO_WARP,
            0,
            0,
            dynamicAtlas,
            warpCache,
            options
        );
        return result == null ? null : result.getRegion();
    }

    public void putDynamicBase(final int spriteIndex, final AtlasPlacement placement) {
        tileLookup.putBase(spriteIndex, placement);
    }

    public void createDynamicHighlight(
        final int spriteIndex,
        final int tileWidth,
        final int tileHeight
    ) {
        final AtlasPlacement placement = dynamicAtlas.createSprite(
            tileWidth,
            tileHeight,
            null,
            fillBlue(tileWidth, tileHeight)
        );
        tileLookup.putBase(spriteIndex, placement);
    }

    private static SpriteBlitter fillBlue(final int width, final int height) {
        return (destination, destX, destY, w, h) -> {
            final Pixmap fill = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            fill.setColor(0f, 0f, 127f / 255f, 127f / 255f);
            fill.fill();
            destination.drawPixmap(fill, destX, destY);
            fill.dispose();
        };
    }

    public Optional<Pixmap> copyBaseSpritePixels(final int spriteIndex) {
        if (!isDynamicAtlas()) {
            return Optional.empty();
        }
        final Optional<TileLookupEntry> base = tileLookup.findBase(spriteIndex);
        if (!base.isPresent()) {
            return Optional.empty();
        }
        final Optional<DynamicAtlas.ReadbackSlice> slice = dynamicAtlas.readbackSlice(
            base.get().getPlacement()
        );
        return slice.isPresent() ? Optional.of(slice.get().copyPixels()) : Optional.empty();
    }

    public void dispose() {
        if (bakedTables != null) {
            bakedTables.dispose();
        }
        if (dynamicAtlas != null) {
            dynamicAtlas.dispose();
        }
        if (warpCache != null) {
            warpCache.clear();
        }
    }
}
