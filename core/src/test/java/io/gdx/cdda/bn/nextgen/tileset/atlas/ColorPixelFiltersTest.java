package io.gdx.cdda.bn.nextgen.tileset.atlas;

import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ColorPixelFiltersTest {

    @BeforeAll
    static void initGdx() {
        GdxTestSupport.initIfNeeded();
    }

    @Test
    void grayscalePreservesBlack() {
        final ColorPixelFilter filter = ColorPixelFilters.grayscale();
        final int black = filter.apply(0, 0, 0, 255);
        assertEquals(ColorPixelFilters.pack(0, 0, 0, 255), black);
    }

    @Test
    void grayscaleMapsColorToGray() {
        final ColorPixelFilter filter = ColorPixelFilters.grayscale();
        final int filtered = filter.apply(255, 0, 0, 255);
        final int red = (filtered >>> 24) & 0xff;
        final int green = (filtered >>> 16) & 0xff;
        final int blue = (filtered >>> 8) & 0xff;
        assertEquals(red, green);
        assertEquals(green, blue);
        assertNotEquals(255, red);
    }

    @Test
    void transparentPixelsSkippedByPixmapApply() {
        final com.badlogic.gdx.graphics.Pixmap pixmap =
            new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.drawPixel(0, 0, ColorPixelFilters.pack(255, 0, 0, 0));
        ColorPixelFilters.applyToPixmap(pixmap, ColorPixelFilters.grayscale());
        assertEquals(0, pixmap.getPixel(0, 0) & 0xff);
    }

    @Test
    void zOverlayStaticEffectMixesSourceColor() {
        final ColorPixelFilter dynamicFilter = ColorPixelFilters.zOverlay(false);
        final ColorPixelFilter staticFilter = ColorPixelFilters.zOverlay(true);
        final int dynamic = dynamicFilter.apply(10, 20, 30, 128);
        final int statik = staticFilter.apply(10, 20, 30, 128);
        assertNotEquals(dynamic, statik);
    }
}
