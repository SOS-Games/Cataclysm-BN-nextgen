package io.gdx.cdda.bn.nextgen.tileset.model;

import com.badlogic.gdx.graphics.Pixmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Per-frame UV warp composite cache (A1). */
public final class WarpCache {

    public static final class WarpSurface {
        private final Pixmap pixmap;
        private final int offsetX;
        private final int offsetY;
        private final boolean offsetMode;

        public WarpSurface(
            final Pixmap pixmap,
            final int offsetX,
            final int offsetY,
            final boolean offsetMode
        ) {
            this.pixmap = pixmap;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetMode = offsetMode;
        }

        public Pixmap getPixmap() {
            return pixmap;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public boolean isOffsetMode() {
            return offsetMode;
        }
    }

    private final Map<Long, WarpSurface> entries = new HashMap<>();

    public long registerWarpSurface(
        final Pixmap composite,
        final int offsetX,
        final int offsetY,
        final boolean offsetMode
    ) {
        final long hash = normalizeWarpContentHash(
            io.gdx.cdda.bn.nextgen.tileset.atlas.PixmapContentHash.hash(composite)
        );
        entries.put(hash, new WarpSurface(composite, offsetX, offsetY, offsetMode));
        return hash;
    }

    public static long normalizeWarpContentHash(final long hash) {
        return hash == TileLookupKey.NO_WARP ? 1L : hash;
    }

    public Optional<WarpSurface> getWarpSurface(final long warpHash) {
        return Optional.ofNullable(entries.get(warpHash));
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
