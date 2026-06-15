package io.gdx.cdda.bn.nextgen.tileset.validate;

import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.WeightedSpriteList;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostLoadValidatorTest {

    @Test
    void stripsOutOfRangeIndices() {
        final WeightedSpriteList list = new WeightedSpriteList();
        final List<Integer> frames = new ArrayList<>();
        frames.add(10);
        frames.add(9999);
        frames.add(12);
        list.add(frames, 1);

        PostLoadValidator.processVariations(list, 100);

        assertEquals(2, list.getVariants().get(0).getFrames().size());
        assertEquals(10, list.getVariants().get(0).getFrames().get(0).intValue());
        assertEquals(12, list.getVariants().get(0).getFrames().get(1).intValue());
        assertEquals(1, list.getTotalWeight());
    }

    @Test
    void removesTileWhenBothSidesEmptyAfterCleanup() {
        final Map<String, TileDefinition> tiles = new LinkedHashMap<>();
        final TileDefinition tile = new TileDefinition("bad_tile");
        tile.getSprites().getForeground().add(singleFrame(5000), 1);
        tiles.put("bad_tile", tile);
        tiles.put(PostLoadValidator.UNKNOWN_TILE_ID, new TileDefinition(PostLoadValidator.UNKNOWN_TILE_ID));
        tiles.put(PostLoadValidator.HIGHLIGHT_ITEM_ID, new TileDefinition(PostLoadValidator.HIGHLIGHT_ITEM_ID));

        final IteratorPruneResult pruned = pruneEmptyTiles(tiles, 100);
        assertFalse(pruned.tiles.containsKey("bad_tile"));
        assertTrue(pruned.warnings.stream().anyMatch(w -> w.contains("bad_tile")));
    }

    @Test
    void precalcBuildsWeightedTotal() {
        final WeightedSpriteList list = new WeightedSpriteList();
        list.add(singleFrame(1), 3);
        list.add(singleFrame(2), 1);
        list.precalc();
        assertEquals(4, list.getTotalWeight());
    }

    private static List<Integer> singleFrame(final int index) {
        final List<Integer> frames = new ArrayList<>(1);
        frames.add(index);
        return frames;
    }

    /** Mirrors phase 1b prune without highlight GPU upload. */
    private static IteratorPruneResult pruneEmptyTiles(final Map<String, TileDefinition> tiles, final int spriteCount) {
        final List<String> warnings = new ArrayList<>();
        final java.util.Iterator<Map.Entry<String, TileDefinition>> iterator = tiles.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, TileDefinition> entry = iterator.next();
            final TileDefinition tile = entry.getValue();
            PostLoadValidator.processVariations(tile.getSprites().getForeground(), spriteCount);
            PostLoadValidator.processVariations(tile.getSprites().getBackground(), spriteCount);
            if (tile.getSprites().getForeground().isEmpty() && tile.getSprites().getBackground().isEmpty()) {
                warnings.add("tile " + entry.getKey() + " has no (valid) foreground nor background");
                iterator.remove();
            }
        }
        return new IteratorPruneResult(tiles, warnings);
    }

    private static final class IteratorPruneResult {
        private final Map<String, TileDefinition> tiles;
        private final List<String> warnings;

        private IteratorPruneResult(final Map<String, TileDefinition> tiles, final List<String> warnings) {
            this.tiles = tiles;
            this.warnings = warnings;
        }
    }
}
