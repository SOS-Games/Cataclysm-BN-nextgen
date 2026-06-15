package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads a sheet PNG into a {@link Pixmap} (unit 06a). */
public final class PixmapSheetLoader {

    private PixmapSheetLoader() {}

    public static Pixmap load(final Path imagePath, final TransparencyKey transparencyKey) throws IOException {
        if (!Files.isRegularFile(imagePath)) {
            throw new IOException("Sheet image not found: " + imagePath);
        }
        final FileHandle handle = Gdx.files.absolute(imagePath.toAbsolutePath().toString());
        final Pixmap pixmap;
        try {
            pixmap = new Pixmap(handle);
        } catch (final Exception e) {
            throw new IOException("Failed to read sheet image: " + imagePath, e);
        }
        if (transparencyKey.isEnabled()) {
            applyColorKey(pixmap, transparencyKey);
        }
        return pixmap;
    }

    /**
     * BN enables color key on black regardless of configured RGB; ports may use configured RGB.
     * This implementation uses black (0,0,0) to match BN.
     */
    public static void applyColorKey(final Pixmap pixmap, final TransparencyKey transparencyKey) {
        if (!transparencyKey.isEnabled()) {
            return;
        }
        pixmap.setBlending(Pixmap.Blending.None);
        final int width = pixmap.getWidth();
        final int height = pixmap.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int rgba = pixmap.getPixel(x, y);
                final int alpha = rgba & 0xff;
                if (alpha == 0) {
                    continue;
                }
                final int r = (rgba >>> 24) & 0xff;
                final int g = (rgba >>> 16) & 0xff;
                final int b = (rgba >>> 8) & 0xff;
                if (r == 0 && g == 0 && b == 0) {
                    pixmap.drawPixel(x, y, 0);
                }
            }
        }
    }

    public static Pixmap extractRegion(
        final Pixmap source,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        final Pixmap region = new Pixmap(width, height, source.getFormat());
        region.drawPixmap(source, 0, 0, x, y, width, height);
        return region;
    }
}
