package io.gdx.cdda.bn.nextgen.view;

import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** Draw-time {@code looks_like} chain resolution (R2). */
public final class TileLooksLikeResolver {

    public static final int DEFAULT_JUMP_LIMIT = 10;

    private TileLooksLikeResolver() {}

    /** Canonical terrain id for multitile neighbor matching (full game-data chain). */
    public static String resolveTerrainConnectId(final String terrainId, final TerrainRegistry terrains) {
        if (terrainId == null || terrainId.isEmpty() || terrains == null) {
            return terrainId;
        }
        final Set<String> seen = new HashSet<>();
        String current = terrainId;
        seen.add(current);
        for (int jump = 0; jump < DEFAULT_JUMP_LIMIT; jump++) {
            final String target = terrainLooksLike(terrains, current);
            if (target == null || seen.contains(target)) {
                break;
            }
            seen.add(target);
            current = target;
        }
        return current;
    }

    /** First id in chain with drawable tileset art, else {@code startId}. */
    public static String resolveTerrainDrawId(
        final String terrainId,
        final LoadedTileset tileset,
        final TerrainRegistry terrains
    ) {
        if (terrains == null) {
            return resolveChain(terrainId, tileset, ignored -> null);
        }
        return resolveChain(terrainId, tileset, id -> terrainLooksLike(terrains, id));
    }

    public static String resolveFurnitureDrawId(
        final String furnitureId,
        final LoadedTileset tileset,
        final FurnitureRegistry furniture
    ) {
        if (furniture == null) {
            return resolveChain(furnitureId, tileset, ignored -> null);
        }
        return resolveChain(furnitureId, tileset, id -> furnitureLooksLike(furniture, id));
    }

    public static boolean hasDrawableTerrainArt(
        final LoadedTileset tileset,
        final String terrainId,
        final TerrainRegistry terrains
    ) {
        if (tileset == null || terrainId == null || terrainId.isEmpty()) {
            return false;
        }
        return TileSpriteResolver.hasDrawableArt(
            tileset,
            resolveTerrainDrawId(terrainId, tileset, terrains)
        );
    }

    public static boolean hasDrawableFurnitureArt(
        final LoadedTileset tileset,
        final String furnitureId,
        final FurnitureRegistry furniture
    ) {
        if (tileset == null || furnitureId == null || furnitureId.isEmpty()) {
            return false;
        }
        return TileSpriteResolver.hasDrawableArt(
            tileset,
            resolveFurnitureDrawId(furnitureId, tileset, furniture)
        );
    }

    public static String resolveChain(
        final String startId,
        final LoadedTileset tileset,
        final Function<String, String> nextLooksLike
    ) {
        if (startId == null || startId.isEmpty() || tileset == null) {
            return startId;
        }
        if (TileSpriteResolver.hasDrawableArt(tileset, startId)) {
            return startId;
        }
        if (nextLooksLike == null) {
            return startId;
        }

        final Set<String> seen = new HashSet<>();
        seen.add(startId);
        String current = startId;
        for (int jump = 0; jump < DEFAULT_JUMP_LIMIT; jump++) {
            final String target = nextLooksLike.apply(current);
            if (target == null || target.isEmpty() || seen.contains(target)) {
                break;
            }
            seen.add(target);
            if (TileSpriteResolver.hasDrawableArt(tileset, target)) {
                return target;
            }
            current = target;
        }
        return startId;
    }

    private static String terrainLooksLike(final TerrainRegistry terrains, final String id) {
        return terrains.find(id)
            .map(TerrainDefinition::getLooksLike)
            .filter(TileLooksLikeResolver::nonEmpty)
            .orElse(null);
    }

    private static String furnitureLooksLike(final FurnitureRegistry furniture, final String id) {
        return furniture.find(id)
            .map(FurnitureDefinition::getLooksLike)
            .filter(TileLooksLikeResolver::nonEmpty)
            .orElse(null);
    }

    private static boolean nonEmpty(final String value) {
        return value != null && !value.isEmpty();
    }
}
