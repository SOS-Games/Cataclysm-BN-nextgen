package io.gdx.cdda.bn.nextgen.tileset.load;

/** GPU upload limits and bake-time effect options (units 06b, 06c, A1). */
public final class TilesetLoadOptions {

    public enum MemoryMapMode {
        SEPIA,
        DARKEN
    }

    private static final int DEFAULT_MIN_TILE_X_COUNT = 128;
    private static final int DEFAULT_MIN_TILE_Y_COUNT = DEFAULT_MIN_TILE_X_COUNT * 2;

    private final int maxTextureWidth;
    private final int maxTextureHeight;
    private final MemoryMapMode memoryMapMode;
    private final boolean staticZEffect;
    private final TilesetLoadMode loadMode;

    public TilesetLoadOptions(
        final int maxTextureWidth,
        final int maxTextureHeight,
        final MemoryMapMode memoryMapMode,
        final boolean staticZEffect,
        final TilesetLoadMode loadMode
    ) {
        this.maxTextureWidth = maxTextureWidth;
        this.maxTextureHeight = maxTextureHeight;
        this.memoryMapMode = memoryMapMode;
        this.staticZEffect = staticZEffect;
        this.loadMode = loadMode;
    }

    public static TilesetLoadOptions defaults() {
        return new TilesetLoadOptions(0, 0, MemoryMapMode.SEPIA, false, TilesetLoadMode.BAKED_TABLES);
    }

    public static TilesetLoadOptions fromGpuMaxSize(final int maxSize) {
        return new TilesetLoadOptions(maxSize, maxSize, MemoryMapMode.SEPIA, false, TilesetLoadMode.BAKED_TABLES);
    }

    public static TilesetLoadOptions forSoftwareRenderer(final int spriteWidth, final int spriteHeight) {
        return new TilesetLoadOptions(
            spriteWidth, spriteHeight, MemoryMapMode.SEPIA, false, TilesetLoadMode.BAKED_TABLES
        );
    }

    public static TilesetLoadOptions dynamicAtlas() {
        return defaults().withLoadMode(TilesetLoadMode.DYNAMIC_ATLAS);
    }

    public TilesetLoadOptions withMemoryMapMode(final MemoryMapMode mode) {
        return new TilesetLoadOptions(maxTextureWidth, maxTextureHeight, mode, staticZEffect, loadMode);
    }

    public TilesetLoadOptions withStaticZEffect(final boolean value) {
        return new TilesetLoadOptions(maxTextureWidth, maxTextureHeight, memoryMapMode, value, loadMode);
    }

    public TilesetLoadOptions withLoadMode(final TilesetLoadMode mode) {
        return new TilesetLoadOptions(maxTextureWidth, maxTextureHeight, memoryMapMode, staticZEffect, mode);
    }

    public int getMaxTextureWidth() {
        return maxTextureWidth;
    }

    public int getMaxTextureHeight() {
        return maxTextureHeight;
    }

    public MemoryMapMode getMemoryMapMode() {
        return memoryMapMode;
    }

    public boolean isStaticZEffect() {
        return staticZEffect;
    }

    public TilesetLoadMode getLoadMode() {
        return loadMode;
    }

    public int resolveMaxTextureWidth(final int spriteWidth) {
        if (maxTextureWidth <= 0) {
            return spriteWidth * DEFAULT_MIN_TILE_X_COUNT;
        }
        return maxTextureWidth;
    }

    public int resolveMaxTextureHeight(final int spriteHeight) {
        if (maxTextureHeight <= 0) {
            return spriteHeight * DEFAULT_MIN_TILE_Y_COUNT;
        }
        return maxTextureHeight;
    }
}
