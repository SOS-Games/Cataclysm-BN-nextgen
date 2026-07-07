package io.gdx.cdda.bn.nextgen.view;

import java.util.Optional;

import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;

/** Resolves BN overmap terrain ids to loaded tileset sprites (BN {@code draw_om} parity). */
public final class OvermapOmtSpriteResolver {

    private OvermapOmtSpriteResolver() {}

    public static final class Resolve {
        private final String tileId;
        private final int rotationIndex;

        public Resolve(final String tileId, final int rotationIndex) {
            this.tileId = tileId;
            this.rotationIndex = rotationIndex;
        }

        public String getTileId() {
            return tileId;
        }

        public int getRotationIndex() {
            return rotationIndex;
        }
    }

    /** Tile id + quarter-turn index for drawing; empty when the tileset has no art. */
    public static Optional<Resolve> resolve(final LoadedTileset tileset, final String omtId) {
        if (tileset == null || omtId == null || omtId.isEmpty()) {
            return Optional.empty();
        }
        final Optional<String> exact = drawableTileId(tileset, omtId);
        if (exact.isPresent()) {
            return Optional.of(new Resolve(exact.get(), 0));
        }
        final String baseId = OvermapTerrainResolver.stripRotation(omtId);
        if (baseId.equals(omtId)) {
            return Optional.empty();
        }
        return drawableTileId(tileset, baseId)
            .map(id -> new Resolve(id, MapGridRotator.rotationFromOmSuffix(omtId)));
    }

    public static Optional<TileDefinition> resolveTile(final LoadedTileset tileset, final String omtId) {
        return resolve(tileset, omtId).flatMap(r -> tileset.findTile(r.getTileId()));
    }

    private static Optional<String> drawableTileId(final LoadedTileset tileset, final String tileId) {
        if (TileSpriteResolver.hasDrawableArt(tileset, tileId)) {
            return Optional.of(tileId);
        }
        return Optional.empty();
    }
}
