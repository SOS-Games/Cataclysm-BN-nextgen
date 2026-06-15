package io.gdx.cdda.bn.nextgen.tileset.model;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.EnumMap;
import java.util.Map;

/** Eight parallel sprite tables baked at load time (unit 06c). */
public final class SpriteTextureTables {

    private final EnumMap<TilesetFxType, SpriteTextureTable> bakedTables;

    public SpriteTextureTables() {
        bakedTables = new EnumMap<>(TilesetFxType.class);
        bakedTables.put(TilesetFxType.NONE, new SpriteTextureTable());
        bakedTables.put(TilesetFxType.SHADOW, new SpriteTextureTable());
        bakedTables.put(TilesetFxType.NIGHT, new SpriteTextureTable());
        bakedTables.put(TilesetFxType.OVEREXPOSED, new SpriteTextureTable());
        bakedTables.put(TilesetFxType.UNDERWATER, new SpriteTextureTable());
        bakedTables.put(TilesetFxType.UNDERWATER_DARK, new SpriteTextureTable());
        bakedTables.put(TilesetFxType.Z_OVERLAY, new SpriteTextureTable());
        bakedTables.put(TilesetFxType.MEMORY, new SpriteTextureTable());
    }

    public SpriteTextureTable getTable(final TilesetFxType fxType) {
        final TilesetFxType resolved = resolveFxType(fxType);
        return bakedTables.get(resolved);
    }

    public TextureRegion getRegion(final int spriteIndex, final TilesetFxType fxType) {
        return getTable(fxType).getRegion(spriteIndex);
    }

    public void ensureCapacity(final int requiredSize) {
        for (final SpriteTextureTable table : bakedTables.values()) {
            table.ensureCapacity(requiredSize);
        }
    }

    public int size() {
        return bakedTables.get(TilesetFxType.NONE).size();
    }

    public void dispose() {
        for (final SpriteTextureTable table : bakedTables.values()) {
            table.dispose();
        }
        bakedTables.clear();
    }

    public Map<TilesetFxType, SpriteTextureTable> getBakedTables() {
        return bakedTables;
    }

    public static TilesetFxType resolveFxType(final TilesetFxType fxType) {
        if (fxType == TilesetFxType.ENHANCED_NIGHT) {
            return TilesetFxType.NIGHT;
        }
        if (fxType == TilesetFxType.ENHANCED_OVEREXPOSED) {
            return TilesetFxType.OVEREXPOSED;
        }
        if (fxType == TilesetFxType.NONE
            || fxType == TilesetFxType.SHADOW
            || fxType == TilesetFxType.NIGHT
            || fxType == TilesetFxType.OVEREXPOSED
            || fxType == TilesetFxType.UNDERWATER
            || fxType == TilesetFxType.UNDERWATER_DARK
            || fxType == TilesetFxType.MEMORY
            || fxType == TilesetFxType.Z_OVERLAY) {
            return fxType;
        }
        return TilesetFxType.NONE;
    }
}
