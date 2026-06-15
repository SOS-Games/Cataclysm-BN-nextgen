package io.gdx.cdda.bn.nextgen.tileset.load;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.atlas.DynamicSheetUploader;
import io.gdx.cdda.bn.nextgen.tileset.atlas.PixmapSheetLoader;
import io.gdx.cdda.bn.nextgen.tileset.atlas.SheetTextureUploader;
import io.gdx.cdda.bn.nextgen.tileset.atlas.TransparencyKey;
import io.gdx.cdda.bn.nextgen.tileset.model.StateModifierRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetTextures;
import io.gdx.cdda.bn.nextgen.tileset.parse.StateModifierParser;
import io.gdx.cdda.bn.nextgen.tileset.parse.TileRegistrar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable state shared across base and mod tileset loads (units 05, 04f). */
public final class TilesetLoadContext {

    private final TileInfo tileInfo;
    private final TilesetTextures textures;
    private final Map<String, TileDefinition> tiles;
    private final StateModifierRegistry stateModifiers;
    private final TilesetLoadOptions options;
    private int offset;

    public TilesetLoadContext(
        final TileInfo tileInfo,
        final TilesetTextures textures,
        final Map<String, TileDefinition> tiles,
        final TilesetLoadOptions options
    ) {
        this.tileInfo = tileInfo;
        this.textures = textures;
        this.tiles = tiles;
        this.stateModifiers = new StateModifierRegistry();
        this.options = options;
        this.offset = 0;
    }

    public TileInfo getTileInfo() {
        return tileInfo;
    }

    public TilesetTextures getTextures() {
        return textures;
    }

    public void finishAtlasLoad() {
        textures.finishLoad();
    }

    public Map<String, TileDefinition> getTiles() {
        return tiles;
    }

    public StateModifierRegistry getStateModifiers() {
        return stateModifiers;
    }

    public TilesetLoadOptions getOptions() {
        return options;
    }

    public int getOffset() {
        return offset;
    }

    public void addOffset(final int spriteCount) {
        offset += spriteCount;
    }

    public void registerSheetAfterUpload(
        final JsonValue sheetConfig,
        final int spriteIdOffset,
        final boolean legacySheet
    ) throws IOException {
        if (legacySheet) {
            TileRegistrar.registerFromConfig(
                sheetConfig,
                tiles,
                TileRegistrar.SheetContext.legacyDefaults(),
                spriteIdOffset
            );
            return;
        }
        if (sheetConfig.has("tiles")) {
            TileRegistrar.registerFromConfig(
                sheetConfig,
                tiles,
                TileRegistrar.SheetContext.fromSheet(sheetConfig),
                spriteIdOffset
            );
        }
        final TileRegistrar.SheetContext sheetContext = TileRegistrar.SheetContext.fromSheet(sheetConfig);
        StateModifierParser.loadFromSheet(sheetConfig, stateModifiers, spriteIdOffset, sheetContext);
    }

    public void loadInternal(
        final JsonValue config,
        final Path tilesetRoot,
        final Path baseLegacyImagePath,
        final int spriteIdOffset
    ) throws IOException {
        if (config.has("tiles-new")) {
            for (JsonValue sheet = config.get("tiles-new").child; sheet != null; sheet = sheet.next) {
                offset += loadTilesNewSheet(tilesetRoot, sheet, spriteIdOffset);
            }
        } else if (config.has("tiles")) {
            offset += loadLegacySheet(tilesetRoot, config, baseLegacyImagePath, spriteIdOffset);
        }
    }

    private int loadTilesNewSheet(
        final Path tilesetRoot,
        final JsonValue sheet,
        final int spriteIdOffset
    ) throws IOException {
        final String fileName = sheet.getString("file", "");
        if (fileName.isEmpty()) {
            throw new IOException("tiles-new sheet missing required \"file\" under " + tilesetRoot);
        }
        final Path imagePath = tilesetRoot.resolve(fileName).normalize();
        final int spriteWidth = sheet.getInt("sprite_width", tileInfo.getWidth());
        final int spriteHeight = sheet.getInt("sprite_height", tileInfo.getHeight());
        final TransparencyKey transparencyKey = transparencyForSheet(sheet);
        final int size = loadSheetImage(imagePath, spriteWidth, spriteHeight, transparencyKey, offset);
        if (sheet.has("tiles")) {
            TileRegistrar.registerFromConfig(
                sheet,
                tiles,
                TileRegistrar.SheetContext.fromSheet(sheet),
                spriteIdOffset
            );
        }
        final TileRegistrar.SheetContext sheetContext = TileRegistrar.SheetContext.fromSheet(sheet);
        StateModifierParser.loadFromSheet(sheet, stateModifiers, spriteIdOffset, sheetContext);
        return size;
    }

    private int loadLegacySheet(
        final Path tilesetRoot,
        final JsonValue root,
        final Path baseLegacyImagePath,
        final int spriteIdOffset
    ) throws IOException {
        final Path imagePath = baseLegacyImagePath != null ? baseLegacyImagePath : tilesetRoot;
        final int size = loadSheetImage(
            imagePath,
            tileInfo.getWidth(),
            tileInfo.getHeight(),
            TransparencyKey.disabled(),
            offset
        );
        TileRegistrar.registerFromConfig(
            root,
            tiles,
            TileRegistrar.SheetContext.legacyDefaults(),
            spriteIdOffset
        );
        return size;
    }

    private int loadSheetImage(
        final Path imagePath,
        final int spriteWidth,
        final int spriteHeight,
        final TransparencyKey transparencyKey,
        final int sheetOffset
    ) throws IOException {
        final Pixmap pixmap = PixmapSheetLoader.load(imagePath, transparencyKey);
        try {
            if (textures.isDynamicAtlas()) {
                return DynamicSheetUploader.uploadSheet(
                    pixmap,
                    spriteWidth,
                    spriteHeight,
                    sheetOffset,
                    textures.getDynamicAtlas(),
                    textures.getTileLookup()
                );
            }
            return SheetTextureUploader.uploadSheet(
                pixmap,
                spriteWidth,
                spriteHeight,
                sheetOffset,
                options.getMaxTextureWidth(),
                options.getMaxTextureHeight(),
                textures.getBakedTables(),
                options
            );
        } catch (final RuntimeException e) {
            throw new IOException("Failed to create texture atlas for " + imagePath, e);
        } finally {
            pixmap.dispose();
        }
    }

    static TransparencyKey transparencyForSheet(final JsonValue sheet) {
        if (!sheet.has("transparency")) {
            return TransparencyKey.disabled();
        }
        final JsonValue transparency = sheet.get("transparency");
        return TransparencyKey.fromChannels(
            transparency.getInt("R", -1),
            transparency.getInt("G", -1),
            transparency.getInt("B", -1)
        );
    }

    public static TilesetLoadContext create(final TileInfo tileInfo, final TilesetLoadOptions options) {
        final TilesetTextures textures = TilesetTextures.create(
            options,
            tileInfo.getWidth(),
            tileInfo.getHeight()
        );
        textures.beginLoad();
        return new TilesetLoadContext(
            tileInfo,
            textures,
            new LinkedHashMap<String, TileDefinition>(),
            options
        );
    }
}
