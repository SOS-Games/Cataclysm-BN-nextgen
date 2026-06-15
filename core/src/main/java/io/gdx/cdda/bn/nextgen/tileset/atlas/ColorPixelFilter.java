package io.gdx.cdda.bn.nextgen.tileset.atlas;

/** Per-pixel RGBA filter applied when baking effect tables (unit 06c). */
public interface ColorPixelFilter {

    int apply(int red, int green, int blue, int alpha);
}
