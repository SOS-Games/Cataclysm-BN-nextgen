package io.gdx.cdda.bn.nextgen.tileset.load;

/** Sprite storage strategy at load and draw time (unit A1 / 06c). */
public enum TilesetLoadMode {
    /** Eight parallel filtered tables baked at load (default). */
    BAKED_TABLES,
    /** Single growable atlas with lazy effect compositing (BN {@code DYNAMIC_ATLAS}). */
    DYNAMIC_ATLAS
}
