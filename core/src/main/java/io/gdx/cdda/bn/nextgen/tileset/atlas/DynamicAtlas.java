package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Growable GPU atlas with CPU batch surfaces (unit A1). */
public final class DynamicAtlas {

    private static final class SpriteSheet {
        private Texture texture;
        private final Pixmap surface;
        private final StripeTexturePacker packer;
        private boolean dirty;

        SpriteSheet(
            final int maxWidth,
            final int maxHeight,
            final int hintSpriteWidth,
            final int hintSpriteHeight
        ) {
            surface = new Pixmap(maxWidth, maxHeight, Pixmap.Format.RGBA8888);
            surface.setColor(0f, 0f, 0f, 0f);
            surface.fill();
            packer = new StripeTexturePacker(
                maxWidth,
                maxHeight,
                Math.max(1, hintSpriteWidth)
            );
            dirty = true;
        }

        void dispose() {
            if (texture != null) {
                texture.dispose();
                texture = null;
            }
            surface.dispose();
        }
    }

    private static final class ReadbackEntry {
        private final Pixmap surface;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        ReadbackEntry(final Pixmap surface, final int x, final int y, final int width, final int height) {
            this.surface = surface;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private final int maxAtlasWidth;
    private final int maxAtlasHeight;
    private final int hintSpriteWidth;
    private final int hintSpriteHeight;
    private final List<SpriteSheet> sheets = new ArrayList<>();
    private final Map<Long, AtlasPlacement> spriteIds = new HashMap<>();
    private final Map<Texture, ReadbackEntry> readbackByTexture = new HashMap<>();
    private final Set<Texture> ownedTextures = new HashSet<>();
    private Pixmap stagingPixmap;
    private boolean batching;
    private boolean readbackLoaded;

    public DynamicAtlas(
        final int maxAtlasWidth,
        final int maxAtlasHeight,
        final int hintSpriteWidth,
        final int hintSpriteHeight
    ) {
        this.maxAtlasWidth = maxAtlasWidth;
        this.maxAtlasHeight = maxAtlasHeight;
        this.hintSpriteWidth = hintSpriteWidth;
        this.hintSpriteHeight = hintSpriteHeight;
    }

    public void startBatch() {
        if (batching) {
            return;
        }
        readbackLoad();
        batching = true;
    }

    public void endBatch() {
        if (!batching) {
            return;
        }
        uploadDirtySheets();
        batching = false;
        readbackLoaded = true;
        rebuildReadbackIndex();
    }

    public void readbackLoad() {
        for (final SpriteSheet sheet : sheets) {
            if (sheet.texture == null) {
                sheet.texture = new Texture(sheet.surface);
                sheet.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                ownedTextures.add(sheet.texture);
            }
        }
        readbackLoaded = true;
        rebuildReadbackIndex();
    }

    public Optional<AtlasPlacement> findSprite(final long contentId) {
        return Optional.ofNullable(spriteIds.get(contentId));
    }

    public AtlasPlacement getOrCreateSprite(
        final int width,
        final int height,
        final Long contentId,
        final SpriteBlitter blitter
    ) {
        if (contentId != null) {
            final AtlasPlacement existing = spriteIds.get(contentId);
            if (existing != null) {
                return existing;
            }
        }
        return createSprite(width, height, contentId, blitter);
    }

    public AtlasPlacement createSprite(
        final int width,
        final int height,
        final Long contentId,
        final SpriteBlitter blitter
    ) {
        final AtlasPlacement placement = allocateSprite(width, height, blitter);
        if (contentId != null) {
            final AtlasPlacement previous = spriteIds.put(contentId, placement);
            if (previous != null) {
                // BN logs hash collision; keep latest placement for the duplicate id.
            }
        }
        return placement;
    }

    public Pixmap getStagingPixmap(final int width, final int height) {
        if (stagingPixmap == null
            || stagingPixmap.getWidth() < width
            || stagingPixmap.getHeight() < height) {
            if (stagingPixmap != null) {
                stagingPixmap.dispose();
            }
            stagingPixmap = new Pixmap(
                Math.max(width, hintSpriteWidth),
                Math.max(height, hintSpriteHeight),
                Pixmap.Format.RGBA8888
            );
        }
        stagingPixmap.setColor(0f, 0f, 0f, 0f);
        stagingPixmap.fill();
        return stagingPixmap;
    }

    public Optional<ReadbackSlice> readbackSlice(final AtlasPlacement placement) {
        final SpriteSheet sheet = findSheet(placement.getTexture());
        if (sheet == null) {
            return Optional.empty();
        }
        return Optional.of(new ReadbackSlice(
            sheet.surface,
            placement.getX(),
            placement.getY(),
            placement.getWidth(),
            placement.getHeight()
        ));
    }

    public Optional<ReadbackSlice> readbackFind(final Texture texture) {
        final ReadbackEntry entry = readbackByTexture.get(texture);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(new ReadbackSlice(entry.surface, entry.x, entry.y, entry.width, entry.height));
    }

    public void dispose() {
        if (stagingPixmap != null) {
            stagingPixmap.dispose();
            stagingPixmap = null;
        }
        for (final SpriteSheet sheet : sheets) {
            sheet.dispose();
        }
        sheets.clear();
        spriteIds.clear();
        readbackByTexture.clear();
        ownedTextures.clear();
    }

    private AtlasPlacement allocateSprite(
        final int width,
        final int height,
        final SpriteBlitter blitter
    ) {
        for (final SpriteSheet sheet : sheets) {
            final Optional<StripeTexturePacker.Rect> packed = sheet.packer.pack(width, height);
            if (packed.isPresent()) {
                final StripeTexturePacker.Rect rect = packed.get();
                blitter.blit(sheet.surface, rect.getX(), rect.getY(), width, height);
                sheet.dirty = true;
                if (!batching) {
                    uploadSheet(sheet);
                    rebuildReadbackIndex();
                }
                return placementFor(sheet, rect);
            }
        }
        final SpriteSheet sheet = new SpriteSheet(
            maxAtlasWidth,
            maxAtlasHeight,
            hintSpriteWidth,
            hintSpriteHeight
        );
        sheets.add(sheet);
        final StripeTexturePacker.Rect rect = sheet.packer.pack(width, height)
            .orElseThrow(() -> new IllegalStateException("sprite larger than atlas page"));
        blitter.blit(sheet.surface, rect.getX(), rect.getY(), width, height);
        sheet.dirty = true;
        if (!batching) {
            uploadSheet(sheet);
            rebuildReadbackIndex();
        }
        return placementFor(sheet, rect);
    }

    private AtlasPlacement placementFor(final SpriteSheet sheet, final StripeTexturePacker.Rect rect) {
        if (sheet.texture == null) {
            uploadSheet(sheet);
        }
        return new AtlasPlacement(
            sheet.texture,
            rect.getX(),
            rect.getY(),
            rect.getWidth(),
            rect.getHeight()
        );
    }

    private void uploadDirtySheets() {
        for (final SpriteSheet sheet : sheets) {
            if (sheet.dirty) {
                uploadSheet(sheet);
                sheet.dirty = false;
            }
        }
    }

    private void uploadSheet(final SpriteSheet sheet) {
        if (sheet.texture != null) {
            sheet.texture.draw(sheet.surface, 0, 0);
            return;
        }
        sheet.texture = new Texture(sheet.surface);
        sheet.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ownedTextures.add(sheet.texture);
    }

    private void rebuildReadbackIndex() {
        readbackByTexture.clear();
        for (final AtlasPlacement placement : spriteIds.values()) {
            final SpriteSheet sheet = findSheet(placement.getTexture());
            if (sheet != null) {
                readbackByTexture.put(
                    placement.getTexture(),
                    new ReadbackEntry(
                        sheet.surface,
                        placement.getX(),
                        placement.getY(),
                        placement.getWidth(),
                        placement.getHeight()
                    )
                );
            }
        }
    }

    private SpriteSheet findSheet(final Texture texture) {
        for (final SpriteSheet sheet : sheets) {
            if (sheet.texture == texture) {
                return sheet;
            }
        }
        return null;
    }

    public boolean isReadbackLoaded() {
        return readbackLoaded;
    }

    public static final class ReadbackSlice {
        private final Pixmap surface;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        ReadbackSlice(final Pixmap surface, final int x, final int y, final int width, final int height) {
            this.surface = surface;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Pixmap copyPixels() {
            final Pixmap copy = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            copy.drawPixmap(surface, 0, 0, x, y, width, height);
            return copy;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
