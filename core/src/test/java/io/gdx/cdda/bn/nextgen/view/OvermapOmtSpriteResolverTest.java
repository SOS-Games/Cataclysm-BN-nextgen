package io.gdx.cdda.bn.nextgen.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import io.gdx.cdda.bn.nextgen.tileset.GdxTestSupport;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteSlot;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TileInfo;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetTextures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OvermapOmtSpriteResolverTest {

    @BeforeAll
    static void initGdx() {
        GdxTestSupport.initIfNeeded();
    }

    @Test
    void resolveUsesExactOmtIdWhenPresent() throws Exception {
        runWithDrawableTileset("forest", tileset -> {
            final OvermapOmtSpriteResolver.Resolve resolved =
                OvermapOmtSpriteResolver.resolve(tileset, "forest").orElseThrow();
            assertEquals("forest", resolved.getTileId());
            assertEquals(0, resolved.getRotationIndex());
        });
    }

    @Test
    void resolveStripsDirectionSuffixAndAppliesRotation() throws Exception {
        runWithDrawableTileset("house_09", tileset -> {
            final OvermapOmtSpriteResolver.Resolve east =
                OvermapOmtSpriteResolver.resolve(tileset, "house_09_east").orElseThrow();
            assertEquals("house_09", east.getTileId());
            assertEquals(1, east.getRotationIndex());

            final OvermapOmtSpriteResolver.Resolve north =
                OvermapOmtSpriteResolver.resolve(tileset, "house_09_north").orElseThrow();
            assertEquals("house_09", north.getTileId());
            assertEquals(0, north.getRotationIndex());
        });
    }

    @Test
    void resolvePrefersExactEntryOverStrippedBase() throws Exception {
        runWithDrawableTileset(new String[] { "house_09_north", "house_09" }, tileset -> {
            final OvermapOmtSpriteResolver.Resolve resolved =
                OvermapOmtSpriteResolver.resolve(tileset, "house_09_north").orElseThrow();
            assertEquals("house_09_north", resolved.getTileId());
            assertEquals(0, resolved.getRotationIndex());
        });
    }

    @Test
    void resolveEmptyWhenNoDrawableArt() throws Exception {
        runWithDrawableTileset("forest", tileset -> {
            assertTrue(OvermapOmtSpriteResolver.resolve(tileset, "unknown_omt").isEmpty());
            assertTrue(OvermapOmtSpriteResolver.resolve(tileset, null).isEmpty());
        });
    }

    private static void runWithDrawableTileset(
        final String drawableId,
        final ThrowingConsumer<LoadedTileset> action
    ) throws Exception {
        runWithDrawableTileset(new String[] { drawableId }, action);
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

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }
}
