package io.gdx.cdda.bn.nextgen.tileset.validate;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import io.gdx.cdda.bn.nextgen.tileset.atlas.ColorPixelFilters;
import io.gdx.cdda.bn.nextgen.tileset.atlas.FilteredTableBake;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteSlot;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteTextureTable;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteTextureTables;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteVariant;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetTextures;
import io.gdx.cdda.bn.nextgen.tileset.model.WeightedSpriteList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Post-load tile cleanup and fallbacks (unit 09). */
public final class PostLoadValidator {

    public static final String HIGHLIGHT_ITEM_ID = "highlight_item";
    public static final String UNKNOWN_TILE_ID = "unknown";

    private static final Logger LOG = Logger.getLogger(PostLoadValidator.class.getName());

    private PostLoadValidator() {}

    public static final class Result {
        private final int spriteCount;
        private final List<String> warnings;

        public Result(final int spriteCount, final List<String> warnings) {
            this.spriteCount = spriteCount;
            this.warnings = warnings;
        }

        public int getSpriteCount() {
            return spriteCount;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    public static Result validate(
        final Map<String, TileDefinition> tiles,
        final TilesetTextures textures,
        final int spriteCount,
        final TileInfo tileInfo,
        final TilesetLoadOptions bakeOptions
    ) {
        final List<String> warnings = new ArrayList<>();
        int finalSpriteCount = spriteCount;

        final Iterator<Map.Entry<String, TileDefinition>> iterator = tiles.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, TileDefinition> entry = iterator.next();
            final TileDefinition tile = entry.getValue();
            processVariations(tile.getSprites().getForeground(), finalSpriteCount);
            processVariations(tile.getSprites().getBackground(), finalSpriteCount);
            if (tile.getSprites().getForeground().isEmpty() && tile.getSprites().getBackground().isEmpty()) {
                warnings.add("tile " + entry.getKey() + " has no (valid) foreground nor background");
                LOG.log(Level.WARNING, "tile {0} has no (valid) foreground nor background", entry.getKey());
                iterator.remove();
            }
        }

        if (!tiles.containsKey(UNKNOWN_TILE_ID)) {
            final String message = "The tileset has no 'unknown' tile defined";
            warnings.add(message);
            LOG.log(Level.WARNING, message);
        }

        if (!tiles.containsKey(HIGHLIGHT_ITEM_ID)) {
            finalSpriteCount = ensureDefaultItemHighlight(
                tiles, textures, finalSpriteCount, tileInfo, bakeOptions
            );
        }

        return new Result(finalSpriteCount, warnings);
    }

    public static void processVariations(final WeightedSpriteList list, final int spriteCount) {
        final List<SpriteVariant> variants = new ArrayList<>(list.getVariants());
        list.clear();
        for (final SpriteVariant variant : variants) {
            final List<Integer> validFrames = new ArrayList<>();
            for (final Integer frame : variant.getFrames()) {
                if (frame != null && frame >= 0 && frame < spriteCount) {
                    validFrames.add(frame);
                }
            }
            if (!validFrames.isEmpty()) {
                list.add(validFrames, variant.getWeight());
            }
        }
        list.precalc();
    }

    private static int ensureDefaultItemHighlight(
        final Map<String, TileDefinition> tiles,
        final TilesetTextures textures,
        final int spriteCount,
        final TileInfo tileInfo,
        final TilesetLoadOptions bakeOptions
    ) {
        final int width = tileInfo.getWidth();
        final int height = tileInfo.getHeight();
        final int index = spriteCount;

        if (textures.isDynamicAtlas()) {
            textures.createDynamicHighlight(index, width, height);
        } else {
            final SpriteTextureTables sprites = textures.getBakedTables();
            final Pixmap basePixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            basePixmap.setColor(0f, 0f, 127f / 255f, 127f / 255f);
            basePixmap.fill();

            for (final FilteredTableBake.Entry bakeEntry : FilteredTableBake.entriesFor(sprites, bakeOptions)) {
                Pixmap uploadPixmap = basePixmap;
                boolean disposeUploadPixmap = false;
                if (bakeEntry.getFilter() != null) {
                    uploadPixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
                    uploadPixmap.drawPixmap(basePixmap, 0, 0);
                    ColorPixelFilters.applyToPixmap(uploadPixmap, bakeEntry.getFilter());
                    disposeUploadPixmap = true;
                }
                try {
                    final Texture texture = new Texture(uploadPixmap);
                    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    final SpriteTextureTable table = bakeEntry.getTable();
                    if (table.size() == index) {
                        table.append(SpriteSlot.of(texture, 0, 0, width, height), texture);
                    } else {
                        table.set(index, SpriteSlot.of(texture, 0, 0, width, height), texture);
                    }
                } finally {
                    if (disposeUploadPixmap) {
                        uploadPixmap.dispose();
                    }
                }
            }
            basePixmap.dispose();
        }

        final TileDefinition highlight = new TileDefinition(HIGHLIGHT_ITEM_ID);
        final List<Integer> frames = new ArrayList<>(1);
        frames.add(index);
        highlight.getSprites().getForeground().add(frames, 1);
        highlight.getSprites().getForeground().precalc();
        tiles.put(HIGHLIGHT_ITEM_ID, highlight);
        return spriteCount + 1;
    }
}
