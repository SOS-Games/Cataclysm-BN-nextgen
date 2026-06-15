package io.gdx.cdda.bn.nextgen.tileset.model;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.tileset.TilesetConfigLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loaded tileset with textures and tile definitions (units 05–09, 06c). */
public final class LoadedTileset {

    private final String tilesetId;
    private final TileInfo tileInfo;
    private final TilesetConfigLoader.ResolvedTilesetPaths paths;
    private final TilesetTextures textures;
    private final Map<String, TileDefinition> tiles;
    private final List<StateModifierGroup> stateModifiers;
    private final int spriteCount;
    private final List<String> loadWarnings;

    public LoadedTileset(
        final String tilesetId,
        final TileInfo tileInfo,
        final TilesetConfigLoader.ResolvedTilesetPaths paths,
        final TilesetTextures textures,
        final Map<String, TileDefinition> tiles,
        final List<StateModifierGroup> stateModifiers,
        final int spriteCount,
        final List<String> loadWarnings
    ) {
        this.tilesetId = tilesetId;
        this.tileInfo = tileInfo;
        this.paths = paths;
        this.textures = textures;
        this.tiles = new LinkedHashMap<>(tiles);
        this.stateModifiers = Collections.unmodifiableList(new ArrayList<>(stateModifiers));
        this.spriteCount = spriteCount;
        this.loadWarnings = loadWarnings;
    }

    public String getTilesetId() {
        return tilesetId;
    }

    public TileInfo getTileInfo() {
        return tileInfo;
    }

    public TilesetConfigLoader.ResolvedTilesetPaths getPaths() {
        return paths;
    }

    public int getSpriteCount() {
        return spriteCount;
    }

    public Map<String, TileDefinition> getTiles() {
        return Collections.unmodifiableMap(tiles);
    }

    public List<StateModifierGroup> getStateModifiers() {
        return stateModifiers;
    }

    public TilesetTextures getTextures() {
        return textures;
    }

    public boolean isDynamicAtlas() {
        return textures.isDynamicAtlas();
    }

    public List<String> getLoadWarnings() {
        return Collections.unmodifiableList(loadWarnings);
    }

    public Optional<TileDefinition> findTile(final String tileId) {
        return Optional.ofNullable(tiles.get(tileId));
    }

    public TextureRegion getTexture(final int spriteIndex) {
        return getTexture(spriteIndex, TilesetFxType.NONE);
    }

    public TextureRegion getTexture(final int spriteIndex, final TilesetFxType fxType) {
        return textures.getRegion(spriteIndex, fxType);
    }

    public TextureRegion getForegroundTexture(final String tileId) {
        return getForegroundTexture(tileId, TilesetFxType.NONE);
    }

    public TextureRegion getForegroundTexture(final String tileId, final TilesetFxType fxType) {
        final TileDefinition tile = tiles.get(tileId);
        if (tile == null) {
            return null;
        }
        final int spriteIndex = tile.getForegroundSpriteIndex();
        if (spriteIndex < 0) {
            return null;
        }
        return getTexture(spriteIndex, fxType);
    }

    public void dispose() {
        textures.dispose();
    }
}
