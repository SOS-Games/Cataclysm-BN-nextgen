package io.gdx.cdda.bn.nextgen.tileset.parse;

import com.badlogic.gdx.utils.JsonReader;

import io.gdx.cdda.bn.nextgen.tileset.model.WeightedSpriteList;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileSpriteListParserTest {

    @Test
    void singleIntWithOffset() throws Exception {
        final WeightedSpriteList list = new WeightedSpriteList();
        TileSpriteListParser.parseInto(parse("{\"fg\": 10}"), list, "fg", 100);
        assertEquals(110, list.getFirstSpriteIndex());
        assertEquals(1, list.getVariants().get(0).getWeight());
    }

    @Test
    void negativeIntIgnored() throws Exception {
        final WeightedSpriteList list = new WeightedSpriteList();
        TileSpriteListParser.parseInto(parse("{\"fg\": -1}"), list, "fg", 0);
        assertTrue(list.isEmpty());
    }

    @Test
    void rotationArray() throws Exception {
        final WeightedSpriteList list = new WeightedSpriteList();
        TileSpriteListParser.parseInto(parse("{\"fg\": [1, 2, 3, 4]}"), list, "fg", 0);
        assertEquals(4, list.getVariants().get(0).getFrames().size());
    }

    @Test
    void weightedSpritePairAccepted() throws Exception {
        final WeightedSpriteList list = new WeightedSpriteList();
        TileSpriteListParser.parseInto(
            parse("{\"fg\": [{\"weight\": 50, \"sprite\": [1, 2]}]}"),
            list,
            "fg",
            0
        );
        assertEquals(50, list.getVariants().get(0).getWeight());
        assertEquals(2, list.getVariants().get(0).getFrames().size());
    }

    @Test
    void weightedInvalidFrameCountThrows() {
        final WeightedSpriteList list = new WeightedSpriteList();
        assertThrows(IOException.class, () ->
            TileSpriteListParser.parseInto(
                parse("{\"fg\": [{\"weight\": 1, \"sprite\": [1, 2, 3]}]}"),
                list,
                "fg",
                0
            )
        );
    }

    @Test
    void negativeWeightThrows() {
        final WeightedSpriteList list = new WeightedSpriteList();
        assertThrows(IOException.class, () ->
            TileSpriteListParser.parseInto(
                parse("{\"fg\": [{\"weight\": -1, \"sprite\": 1}]}"),
                list,
                "fg",
                0
            )
        );
    }

    private static com.badlogic.gdx.utils.JsonValue parse(final String json) {
        return new JsonReader().parse(json);
    }
}
