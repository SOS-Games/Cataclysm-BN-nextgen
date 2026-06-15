package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;

/** BN color_pixel_* filters ported from {@code sdl_utils.cpp} (unit 06c). */
public final class ColorPixelFilters {

    private ColorPixelFilters() {}

    public static void applyToPixmap(final Pixmap pixmap, final ColorPixelFilter filter) {
        if (filter == null) {
            return;
        }
        final int width = pixmap.getWidth();
        final int height = pixmap.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int rgba = pixmap.getPixel(x, y);
                final int alpha = rgba & 0xff;
                if (alpha == 0) {
                    continue;
                }
                final int red = (rgba >>> 24) & 0xff;
                final int green = (rgba >>> 16) & 0xff;
                final int blue = (rgba >>> 8) & 0xff;
                pixmap.drawPixel(x, y, filter.apply(red, green, blue, alpha));
            }
        }
    }

    public static ColorPixelFilter grayscale() {
        return (red, green, blue, alpha) -> {
            if (isBlack(red, green, blue)) {
                return pack(red, green, blue, alpha);
            }
            final int average = averagePixelColor(red, green, blue);
            final int result = Math.max(average * 5 >> 3, 0x01);
            return pack(result, result, result, alpha);
        };
    }

    public static ColorPixelFilter nightVision() {
        return (red, green, blue, alpha) -> {
            final int average = averagePixelColor(red, green, blue);
            final int result = Math.min((average * ((average * 3 >> 2) + 64) >> 8) + 16, 0xff);
            return pack(result >> 2, result, result >> 3, alpha);
        };
    }

    public static ColorPixelFilter overexposed() {
        return (red, green, blue, alpha) -> {
            final int average = averagePixelColor(red, green, blue);
            final int result = Math.min(64 + (average * ((average >> 2) + 0xc0) >> 8), 0xff);
            return pack(result >> 2, result, result >> 3, alpha);
        };
    }

    public static ColorPixelFilter underwater() {
        return (red, green, blue, alpha) -> {
            if (isBlack(red, green, blue)) {
                return pack(red, green, blue, alpha);
            }
            return pack(
                Math.max(175 * red >> 8, 0x01),
                Math.max(225 * green >> 8, 0x01),
                Math.max(250 * blue >> 8, 0x01),
                alpha
            );
        };
    }

    public static ColorPixelFilter underwaterDark() {
        return (red, green, blue, alpha) -> {
            if (isBlack(red, green, blue)) {
                return pack(red, green, blue, alpha);
            }
            return pack(
                Math.max(100 * red >> 8, 0x01),
                Math.max(113 * green >> 8, 0x01),
                Math.max(125 * blue >> 8, 0x01),
                alpha
            );
        };
    }

    public static ColorPixelFilter darken() {
        return (red, green, blue, alpha) -> {
            if (isBlack(red, green, blue)) {
                return pack(red, green, blue, alpha);
            }
            return pack(
                Math.max(85 * red >> 8, 0x01),
                Math.max(85 * green >> 8, 0x01),
                Math.max(85 * blue >> 8, 0x01),
                alpha
            );
        };
    }

    public static ColorPixelFilter sepia() {
        return (red, green, blue, alpha) -> {
            if (isBlack(red, green, blue)) {
                return pack(red, green, blue, alpha);
            }
            final int average = averagePixelColor(red, green, blue);
            final double pv = average / 255.0;
            final int finalValue = Math.min((int) Math.round(Math.pow(pv, 1.6) * 150.0), 100);
            return mixColors(39, 23, 19, alpha, 241, 220, 163, alpha, finalValue);
        };
    }

    public static ColorPixelFilter zOverlay(final boolean staticZEffect) {
        if (staticZEffect) {
            return (red, green, blue, alpha) ->
                mixColors(red, green, blue, alpha, 128, 255, 255, alpha, alpha / 8);
        }
        return (red, green, blue, alpha) -> pack(128, 255, 255, alpha);
    }

    public static int pack(final int red, final int green, final int blue, final int alpha) {
        return (red << 24) | (green << 16) | (blue << 8) | alpha;
    }

    private static boolean isBlack(final int red, final int green, final int blue) {
        return red == 0 && green == 0 && blue == 0;
    }

    private static int averagePixelColor(final int red, final int green, final int blue) {
        return 85 * (red + green + blue) >> 8;
    }

    private static int mixColors(
        final int red1,
        final int green1,
        final int blue1,
        final int alpha1,
        final int red2,
        final int green2,
        final int blue2,
        final int alpha2,
        final int secondPercent
    ) {
        if (secondPercent <= 0) {
            return pack(red1, green1, blue1, alpha1);
        }
        if (secondPercent >= 100) {
            return pack(red2, green2, blue2, alpha2);
        }
        final int firstPercent = 100 - secondPercent;
        final int red = Math.min((red1 * firstPercent + red2 * secondPercent) / 100, 255);
        final int green = Math.min((green1 * firstPercent + green2 * secondPercent) / 100, 255);
        final int blue = Math.min((blue1 * firstPercent + blue2 * secondPercent) / 100, 255);
        final int alpha = Math.min((alpha1 * firstPercent + alpha2 * secondPercent) / 100, 255);
        return pack(red, green, blue, alpha);
    }
}
