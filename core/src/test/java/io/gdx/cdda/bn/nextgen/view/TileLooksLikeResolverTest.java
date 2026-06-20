package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteSlot;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetTextures;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileLooksLikeResolverTest {

    @BeforeAll
    static void initGdx() {
        GdxTestSupport.initIfNeeded();
    }

    @Test
    void resolveChainFindsFirstDrawableTarget() throws Exception {
        runWithDrawableTileset(new String[] { "t_dirt" }, tileset -> {
            final String resolved = TileLooksLikeResolver.resolveChain(
                "t_region_a",
                tileset,
                id -> {
                    if ("t_region_a".equals(id)) {
                        return "t_region_b";
                    }
                    if ("t_region_b".equals(id)) {
                        return "t_dirt";
                    }
                    return null;
                }
            );
            assertEquals("t_dirt", resolved);
        });
    }

    @Test
    void resolveChainStopsOnCycleWithoutArt() throws Exception {
        runWithDrawableTileset(new String[] { "t_dirt" }, tileset -> {
            final String resolved = TileLooksLikeResolver.resolveChain(
                "t_a",
                tileset,
                id -> "t_b".equals(id) ? "t_a" : "t_b"
            );
            assertEquals("t_a", resolved);
        });
    }

    @Test
    void resolveTerrainDrawIdUsesRegistryChain() throws Exception {
        final TerrainRegistry terrains = new TerrainRegistry();
        terrains.put(terrain("t_region_grass", "t_grass"));
        terrains.put(terrain("t_grass", null));

        runWithDrawableTileset(new String[] { "t_grass" }, tileset -> {
            assertEquals(
                "t_grass",
                TileLooksLikeResolver.resolveTerrainDrawId("t_region_grass", tileset, terrains)
            );
        });
    }

    @Test
    void resolveTerrainConnectIdWalksFullChain() {
        final TerrainRegistry terrains = new TerrainRegistry();
        terrains.put(terrain("t_region_grass", "t_grass_alias"));
        terrains.put(terrain("t_grass_alias", "t_grass"));
        terrains.put(terrain("t_grass", null));

        assertEquals(
            "t_grass",
            TileLooksLikeResolver.resolveTerrainConnectId("t_region_grass", terrains)
        );
    }

    @Test
    void resolveFurnitureDrawIdUsesRegistryChain() throws Exception {
        final FurnitureRegistry furniture = new FurnitureRegistry();
        furniture.put(furniture("f_alias_chair", "f_chair"));
        furniture.put(furniture("f_chair", null));

        runWithDrawableTileset(new String[] { "f_chair" }, tileset -> {
            assertEquals(
                "f_chair",
                TileLooksLikeResolver.resolveFurnitureDrawId("f_alias_chair", tileset, furniture)
            );
        });
    }

    @Test
    void hasDrawableTerrainArtTrueWhenChainTargetHasArt() throws Exception {
        final TerrainRegistry terrains = new TerrainRegistry();
        terrains.put(terrain("t_region_grass", "t_grass"));
        terrains.put(terrain("t_grass", null));

        runWithDrawableTileset(new String[] { "t_grass" }, tileset -> {
            assertTrue(TileLooksLikeResolver.hasDrawableTerrainArt(tileset, "t_region_grass", terrains));
        });
    }

    @Test
    void hasDrawableTerrainArtFalseWhenNoArtInChain() throws Exception {
        final TerrainRegistry terrains = new TerrainRegistry();
        terrains.put(terrain("t_region_grass", "t_grass"));
        terrains.put(terrain("t_grass", null));

        runWithDrawableTileset(new String[] { "t_dirt" }, tileset -> {
            assertFalse(TileLooksLikeResolver.hasDrawableTerrainArt(tileset, "t_region_grass", terrains));
        });
    }

    private static void runWithDrawableTileset(
        final String[] drawableIds,
        final ThrowingConsumer<LoadedTileset> action
    ) throws Exception {
        final AtomicReference<LoadedTileset> tilesetRef = new AtomicReference<>();
        GdxTestSupport.runOnGdxThread(() -> tilesetRef.set(buildDrawableTileset(drawableIds)));
        action.accept(tilesetRef.get());
    }

    private static LoadedTileset buildDrawableTileset(final String[] drawableIds) {
        final TilesetTextures textures = TilesetTextures.create(TilesetLoadOptions.defaults(), 8, 8);
        final Pixmap pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 0f, 0f, 1f);
        pixmap.fill();
        final Texture texture = new Texture(pixmap);
        pixmap.dispose();
        textures.getBakedTables().getTable(TilesetFxType.NONE).set(0, SpriteSlot.of(texture, 0, 0, 8, 8), texture);

        final Map<String, TileDefinition> tiles = new LinkedHashMap<>();
        for (final String id : drawableIds) {
            final TileDefinition tile = new TileDefinition(id);
            tile.getSprites().getForeground().add(Collections.singletonList(0), 1);
            tile.getSprites().getForeground().precalc();
            tiles.put(id, tile);
        }
        return new LoadedTileset(
            "test",
            new TileInfo(8, 8, 1f, false),
            null,
            textures,
            tiles,
            Collections.emptyList(),
            1,
            Collections.emptyList()
        );
    }

    private static TerrainDefinition terrain(final String id, final String looksLike) {
        return new TerrainDefinition(
            id,
            id,
            null,
            ".",
            "green",
            2,
            Collections.emptyList(),
            looksLike,
            "test"
        );
    }

    private static FurnitureDefinition furniture(final String id, final String looksLike) {
        return new FurnitureDefinition(
            id,
            id,
            "#",
            "brown",
            0,
            0,
            Collections.emptyList(),
            looksLike,
            "test"
        );
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }
}
