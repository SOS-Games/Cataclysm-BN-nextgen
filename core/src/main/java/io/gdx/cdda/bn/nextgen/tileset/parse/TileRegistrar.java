package io.gdx.cdda.bn.nextgen.tileset.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.WeightedSpriteList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Registers tile JSON entries into {@code tile_ids} (unit 07b). */
public final class TileRegistrar {

    private static final Logger LOG = Logger.getLogger(TileRegistrar.class.getName());

    private static final List<String> MULTITILE_KEYS = Collections.unmodifiableList(
        Arrays.asList(
            "center", "corner", "edge", "t_connection", "end_piece", "unconnected", "open", "broken"
        )
    );

    private TileRegistrar() {}

    public static void registerFromConfig(
        final JsonValue config,
        final Map<String, TileDefinition> tiles,
        final SheetContext sheet,
        final int spriteIdOffset
    ) throws IOException {
        if (!config.has("tiles")) {
            throw new IOException("\"tiles\" section missing");
        }
        final JsonValue tilesArray = config.get("tiles");
        for (JsonValue entry = tilesArray.child; entry != null; entry = entry.next) {
            registerEntry(entry, tiles, sheet, spriteIdOffset);
        }
    }

    private static void registerEntry(
        final JsonValue entry,
        final Map<String, TileDefinition> tiles,
        final SheetContext sheet,
        final int spriteIdOffset
    ) throws IOException {
        final List<String> ids = resolveIds(entry);
        for (final String tileId : ids) {
            registerTileId(entry, tileId, tiles, sheet, spriteIdOffset);
        }
    }

    private static void registerTileId(
        final JsonValue entry,
        final String tileId,
        final Map<String, TileDefinition> tiles,
        final SheetContext sheet,
        final int spriteIdOffset
    ) throws IOException {
        final TileDefinition tile = loadTile(entry, tileId, tiles, spriteIdOffset);
        applySheetContext(tile, sheet);

        final boolean multitile = entry.getBoolean("multitile", false);
        final boolean rotates = entry.getBoolean("rotates", multitile);
        final int height3d = entry.getInt("height_3d", 0);
        final boolean animated = entry.getBoolean("animated", false);

        if (multitile) {
            final JsonValue additionalTiles = entry.get("additional_tiles");
            if (additionalTiles != null && additionalTiles.isArray()) {
                for (JsonValue subentry = additionalTiles.child; subentry != null; subentry = subentry.next) {
                    final String subId = subentry.getString("id", "");
                    if (subId.isEmpty()) {
                        continue;
                    }
                    final String multitileId = tileId + "_" + subId;
                    final TileDefinition subtile = loadTile(subentry, multitileId, tiles, spriteIdOffset);
                    applySheetContext(subtile, sheet);
                    subtile.setRotates(true);
                    subtile.setMultitileSubtile(MULTITILE_KEYS.contains(subId));
                    subtile.setHeight3d(height3d);
                    subtile.setAnimated(subentry.getBoolean("animated", false));
                    tile.addAvailableSubtile(subId);
                }
            }
        } else if (entry.has("additional_tiles")) {
            throw new IOException("Additional tiles defined, but 'multitile' is not true.");
        }

        tile.setMultitile(multitile);
        tile.setRotates(rotates);
        tile.setHeight3d(height3d);
        tile.setAnimated(animated);
        tile.setMultitileSubtile(false);
        tile.setHasOmTransparency(entry.getBoolean("has_om_transparency", false));
    }

    private static TileDefinition loadTile(
        final JsonValue entry,
        final String tileId,
        final Map<String, TileDefinition> tiles,
        final int spriteIdOffset
    ) throws IOException {
        final TileDefinition tile = new TileDefinition(tileId);
        TileSpriteListParser.parseInto(entry, tile.getSprites().getForeground(), "fg", spriteIdOffset);
        TileSpriteListParser.parseInto(entry, tile.getSprites().getBackground(), "bg", spriteIdOffset);
        ensureMasks(entry, tile, tileId, spriteIdOffset);
        tiles.put(tileId, tile);
        return tile;
    }

    private static void ensureMasks(
        final JsonValue entry,
        final TileDefinition tile,
        final String tileId,
        final int spriteIdOffset
    ) throws IOException {
        final WeightedSpriteList maskForeground = new WeightedSpriteList();
        final WeightedSpriteList maskBackground = new WeightedSpriteList();
        if (entry.has("masks")) {
            for (JsonValue maskEntry = entry.get("masks").child; maskEntry != null; maskEntry = maskEntry.next) {
                final String maskType = maskEntry.getString("type", "");
                if ("tint".equals(maskType)) {
                    TileSpriteListParser.parseInto(maskEntry, maskForeground, "fg", spriteIdOffset);
                    TileSpriteListParser.parseInto(maskEntry, maskBackground, "bg", spriteIdOffset);
                } else if (!maskType.isEmpty()) {
                    LOG.log(Level.WARNING, "Invalid tile mask type: {0} for {1}", new Object[] { maskType, tileId });
                }
            }
        }
        ensureMask(maskForeground, tile.getSprites().getForeground(), tileId);
        ensureMask(maskBackground, tile.getSprites().getBackground(), tileId);
    }

    private static void ensureMask(
        final WeightedSpriteList mask,
        final WeightedSpriteList sprites,
        final String tileId
    ) {
        if (!mask.isEmpty()) {
            final List<io.gdx.cdda.bn.nextgen.tileset.model.SpriteVariant> maskVariants = mask.getVariants();
            final List<io.gdx.cdda.bn.nextgen.tileset.model.SpriteVariant> spriteVariants = sprites.getVariants();
            if (maskVariants.size() != spriteVariants.size()) {
                LOG.log(Level.WARNING, "Tile mask definition must match sprite: {0}", tileId);
                mask.clear();
            } else {
                for (int i = 0; i < maskVariants.size(); i++) {
                    if (maskVariants.get(i).getWeight() != spriteVariants.get(i).getWeight()
                        || maskVariants.get(i).getFrames().size() != spriteVariants.get(i).getFrames().size()) {
                        LOG.log(Level.WARNING, "Tile mask definition must match sprite: {0}", tileId);
                        mask.clear();
                        break;
                    }
                }
            }
        }
        // Masks are parsed but not stored on TileDefinition in this slice — auto-fill is BN parity when needed later.
    }

    private static void applySheetContext(final TileDefinition tile, final SheetContext sheet) {
        tile.setOffsetX(sheet.getOffsetX());
        tile.setOffsetY(sheet.getOffsetY());
        tile.setOffsetRetractedX(sheet.getOffsetRetractedX());
        tile.setOffsetRetractedY(sheet.getOffsetRetractedY());
        tile.setPixelScale(sheet.getPixelScale());
    }

    private static List<String> resolveIds(final JsonValue entry) {
        if (!entry.has("id")) {
            return Collections.emptyList();
        }
        final JsonValue idValue = entry.get("id");
        if (idValue.isString()) {
            final String id = idValue.asString();
            if (id.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.singletonList(id);
        }
        if (idValue.isArray()) {
            final List<String> ids = new ArrayList<>();
            for (JsonValue child = idValue.child; child != null; child = child.next) {
                if (child.isString()) {
                    final String id = child.asString();
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
            }
            return ids;
        }
        return Collections.emptyList();
    }

    public static final class SheetContext {
        private final int offsetX;
        private final int offsetY;
        private final int offsetRetractedX;
        private final int offsetRetractedY;
        private final float pixelScale;

        public SheetContext(
            final int offsetX,
            final int offsetY,
            final int offsetRetractedX,
            final int offsetRetractedY,
            final float pixelScale
        ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetRetractedX = offsetRetractedX;
            this.offsetRetractedY = offsetRetractedY;
            this.pixelScale = pixelScale;
        }

        public static SheetContext fromSheet(final JsonValue sheet) {
            final int offsetX = sheet.getInt("sprite_offset_x", 0);
            final int offsetY = sheet.getInt("sprite_offset_y", 0);
            final int offsetRetractedX = sheet.getInt("sprite_offset_x_retracted", offsetX);
            final int offsetRetractedY = sheet.getInt("sprite_offset_y_retracted", offsetY);
            final float pixelScale = sheet.getFloat("pixelscale", 1f);
            return new SheetContext(offsetX, offsetY, offsetRetractedX, offsetRetractedY, pixelScale);
        }

        public static SheetContext legacyDefaults() {
            return new SheetContext(0, 0, 0, 0, 1f);
        }

        public int getOffsetX() {
            return offsetX;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public int getOffsetRetractedX() {
            return offsetRetractedX;
        }

        public int getOffsetRetractedY() {
            return offsetRetractedY;
        }

        public float getPixelScale() {
            return pixelScale;
        }
    }
}
