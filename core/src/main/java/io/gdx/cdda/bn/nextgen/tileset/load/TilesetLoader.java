package io.gdx.cdda.bn.nextgen.tileset.load;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.TilesetConfigLoader;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetMerger;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.parse.TileRegistrar;
import io.gdx.cdda.bn.nextgen.tileset.TilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.validate.PostLoadValidator;

import java.io.IOException;
import java.util.Map;

/** End-to-end tileset load orchestration (units 05, 06, 07, 09, 04f). */
public final class TilesetLoader {

    private TilesetLoader() {}

    public static LoadedTileset load(
        final TilesetRegistry registry,
        final String tilesetId
    ) throws IOException {
        return load(registry, tilesetId, TilesetLoadOptions.defaults(), ModTilesetRegistry.empty());
    }

    public static LoadedTileset load(
        final TilesetRegistry registry,
        final String tilesetId,
        final TilesetLoadOptions options
    ) throws IOException {
        return load(registry, tilesetId, options, ModTilesetRegistry.empty());
    }

    public static LoadedTileset load(
        final TilesetRegistry registry,
        final String tilesetId,
        final TilesetLoadOptions options,
        final ModTilesetRegistry modTilesets
    ) throws IOException {
        final TilesetConfigLoader.LoadedTilesetConfig config =
            TilesetConfigLoader.loadConfig(registry, tilesetId);
        final TileInfo tileInfo = parseTileInfo(config.getConfigRoot());
        final TilesetLoadContext context = TilesetLoadContext.create(tileInfo, options);
        final JsonValue root = config.getConfigRoot();
        final TilesetConfigLoader.ResolvedTilesetPaths paths = config.getPaths();

        context.loadInternal(root, paths.getTilesetRoot(), paths.getImagePath(), 0);
        ModTilesetMerger.mergeCompatible(tilesetId, modTilesets, context, paths.getImagePath());
        context.finishAtlasLoad();

        final PostLoadValidator.Result validated = PostLoadValidator.validate(
            context.getTiles(),
            context.getTextures(),
            context.getOffset(),
            tileInfo,
            options
        );
        return new LoadedTileset(
            tilesetId,
            tileInfo,
            paths,
            context.getTextures(),
            context.getTiles(),
            context.getStateModifiers().getGroups(),
            validated.getSpriteCount(),
            validated.getWarnings()
        );
    }

    public static TileInfo parseTileInfo(final JsonValue configRoot) throws IOException {
        final JsonValue tileInfoArray = configRoot.get("tile_info");
        if (tileInfoArray == null || tileInfoArray.size == 0) {
            throw new IOException("tile_config missing required \"tile_info\"");
        }
        final JsonValue entry = tileInfoArray.child;
        final int width = entry.getInt("width", 0);
        final int height = entry.getInt("height", 0);
        if (width <= 0 || height <= 0) {
            throw new IOException("tile_info width/height must be positive");
        }
        final float pixelScale = entry.getFloat("pixelscale", 1f);
        final boolean iso = entry.getBoolean("iso", false);
        return new TileInfo(width, height, pixelScale, iso);
    }
}
