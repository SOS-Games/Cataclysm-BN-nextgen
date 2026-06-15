package io.gdx.cdda.bn.nextgen.tileset.parse;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.model.StateModifierGroup;
import io.gdx.cdda.bn.nextgen.tileset.model.StateModifierRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.StateModifierTile;
import io.gdx.cdda.bn.nextgen.tileset.parse.TileRegistrar.SheetContext;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateModifierParserTest {

    private static final SheetContext DEFAULT_SHEET = SheetContext.legacyDefaults();

    @Test
    void sheetWithoutStateModifiersLeavesRegistryEmpty() throws Exception {
        final StateModifierRegistry registry = new StateModifierRegistry();
        StateModifierParser.loadFromSheet(parse("{}"), registry, 0, DEFAULT_SHEET);
        assertEquals(0, registry.size());
    }

    @Test
    void oneGroupWithThreeTiles() throws Exception {
        final StateModifierRegistry registry = new StateModifierRegistry();
        StateModifierParser.loadFromSheet(
            parse("{\"state-modifiers\": [{"
                + "\"id\": \"movement_mode\","
                + "\"tiles\": ["
                + "  {\"id\": \"walk\"},"
                + "  {\"id\": \"crouch\", \"fg\": 1},"
                + "  {\"id\": \"run\", \"fg\": null}"
                + "]"
                + "}]}"),
            registry,
            0,
            DEFAULT_SHEET
        );
        assertEquals(1, registry.size());
        assertEquals(3, registry.getGroups().get(0).getTiles().size());
    }

    @Test
    void nullForegroundIsIdentity() throws Exception {
        final StateModifierTile tile = parseTile("{\"id\": \"medium\", \"fg\": null}", 0);
        assertTrue(tile.isIdentity());
        assertFalse(tile.getForegroundSprite().isPresent());
    }

    @Test
    void positiveForegroundAppliesSpriteOffset() throws Exception {
        final StateModifierTile tile = parseTile("{\"id\": \"crouch\", \"fg\": 5}", 100);
        assertEquals(105, tile.getForegroundSprite().get().intValue());
    }

    @Test
    void negativeForegroundIsIdentity() throws Exception {
        final StateModifierTile tile = parseTile("{\"id\": \"crouch\", \"fg\": -3}", 0);
        assertTrue(tile.isIdentity());
    }

    @Test
    void omittedForegroundIsIdentity() throws Exception {
        final StateModifierTile tile = parseTile("{\"id\": \"walk\"}", 0);
        assertTrue(tile.isIdentity());
    }

    @Test
    void arrayForegroundIsIdentity() throws Exception {
        final StateModifierTile tile = parseTile("{\"id\": \"walk\", \"fg\": [1, 2]}", 0);
        assertTrue(tile.isIdentity());
    }

    @Test
    void mergeReplacesInPlaceKeepingIndex() throws Exception {
        final StateModifierRegistry registry = new StateModifierRegistry();
        final JsonValue sheet = parse("{\"state-modifiers\": [{"
            + "\"id\": \"movement_mode\","
            + "\"tiles\": [{\"id\": \"crouch\", \"fg\": 1}]"
            + "}]}");

        StateModifierParser.loadFromSheet(sheet, registry, 0, DEFAULT_SHEET);
        assertEquals(1, registry.getGroups().get(0).getTiles().get("crouch").getForegroundSprite().get().intValue());

        StateModifierParser.loadFromSheet(
            parse("{\"state-modifiers\": [{"
                + "\"id\": \"movement_mode\","
                + "\"tiles\": [{\"id\": \"crouch\", \"fg\": 9}]"
                + "}]}"),
            registry,
            0,
            DEFAULT_SHEET
        );
        assertEquals(1, registry.size());
        assertEquals(9, registry.getGroups().get(0).getTiles().get("crouch").getForegroundSprite().get().intValue());
    }

    @Test
    void sameGroupIdDifferentFiltersAreSeparateEntries() throws Exception {
        final StateModifierRegistry registry = new StateModifierRegistry();
        StateModifierParser.loadFromSheet(
            parse("{\"state-modifiers\": ["
                + "{\"id\": \"movement_mode\", \"tiles\": [{\"id\": \"crouch\", \"fg\": 1}]},"
                + "{\"id\": \"movement_mode\", \"whitelist\": [\"worn_\"], "
                + "\"tiles\": [{\"id\": \"crouch\", \"fg\": 2}]}"
                + "]}"),
            registry,
            0,
            DEFAULT_SHEET
        );
        assertEquals(2, registry.size());
        assertEquals(Collections.emptyList(), registry.getGroups().get(0).getWhitelist());
        assertEquals(Collections.singletonList("worn_"), registry.getGroups().get(1).getWhitelist());
    }

    @Test
    void whitelistNormalizedForMergeKey() throws Exception {
        final StateModifierRegistry registry = new StateModifierRegistry();
        StateModifierParser.loadFromSheet(
            parse("{\"state-modifiers\": [{"
                + "\"id\": \"movement_mode\","
                + "\"whitelist\": [\"b\", \"a\", \"a\"],"
                + "\"tiles\": [{\"id\": \"crouch\", \"fg\": 1}]"
                + "}]}"),
            registry,
            0,
            DEFAULT_SHEET
        );
        assertEquals(Arrays.asList("a", "b"), registry.getGroups().get(0).getWhitelist());
    }

    @Test
    void overrideAndUseOffsetFlagsPreserved() throws Exception {
        final StateModifierRegistry registry = new StateModifierRegistry();
        StateModifierParser.loadFromSheet(
            parse("{\"state-modifiers\": [{"
                + "\"id\": \"downed\","
                + "\"override\": true,"
                + "\"use_offset\": false,"
                + "\"tiles\": [{\"id\": \"downed\", \"fg\": 4}]"
                + "}]}"),
            registry,
            0,
            DEFAULT_SHEET
        );
        final StateModifierGroup group = registry.getGroups().get(0);
        assertTrue(group.isOverrideLower());
        assertFalse(group.isUseOffsetMode());
    }

    @Test
    void omittedOffsetUsesSheetSpriteOffset() throws Exception {
        final SheetContext sheet = SheetContext.fromSheet(
            parse("{\"sprite_offset_x\": -16, \"sprite_offset_y\": -48}")
        );
        final StateModifierTile tile = parseTile("{\"id\": \"crouch\", \"fg\": 1}", 0, sheet);
        assertEquals(-16, tile.getOffsetX());
        assertEquals(-48, tile.getOffsetY());
    }

    @Test
    void modSheetSpriteIdOffsetApplied() throws Exception {
        final StateModifierTile tile = parseTile("{\"id\": \"crouch\", \"fg\": 0}", 150);
        assertEquals(150, tile.getForegroundSprite().get().intValue());
    }

    @Test
    void duplicateStateIdLastWins() throws Exception {
        final StateModifierRegistry registry = new StateModifierRegistry();
        StateModifierParser.loadFromSheet(
            parse("{\"state-modifiers\": [{"
                + "\"id\": \"movement_mode\","
                + "\"tiles\": ["
                + "  {\"id\": \"crouch\", \"fg\": 1},"
                + "  {\"id\": \"crouch\", \"fg\": 2}"
                + "]"
                + "}]}"),
            registry,
            0,
            DEFAULT_SHEET
        );
        assertEquals(2, registry.getGroups().get(0).getTiles().get("crouch").getForegroundSprite().get().intValue());
    }

    @Test
    void groupMissingTilesThrows() {
        final StateModifierRegistry registry = new StateModifierRegistry();
        assertThrows(IOException.class, () ->
            StateModifierParser.loadFromSheet(
                parse("{\"state-modifiers\": [{\"id\": \"movement_mode\"}]}"),
                registry,
                0,
                DEFAULT_SHEET
            )
        );
    }

    @Test
    void normalizeFilterListDedupesAndSorts() {
        final List<String> normalized = StateModifierParser.normalizeFilterList(
            Arrays.asList("b", "a", "a")
        );
        assertEquals(Arrays.asList("a", "b"), normalized);
    }

    @Test
    void resolveForegroundSpriteMatchesSpec() {
        assertNull(StateModifierParser.resolveForegroundSprite(parse("{\"fg\": null}"), 0));
        assertEquals(
            Integer.valueOf(105),
            StateModifierParser.resolveForegroundSprite(parse("{\"fg\": 5}"), 100)
        );
        assertNull(StateModifierParser.resolveForegroundSprite(parse("{\"fg\": -1}"), 0));
        assertNull(StateModifierParser.resolveForegroundSprite(parse("{}"), 0));
    }

    private static StateModifierTile parseTile(final String json, final int spriteIdOffset) throws IOException {
        return parseTile(json, spriteIdOffset, DEFAULT_SHEET);
    }

    private static StateModifierTile parseTile(
        final String json,
        final int spriteIdOffset,
        final SheetContext sheetContext
    ) throws IOException {
        final StateModifierRegistry registry = new StateModifierRegistry();
        StateModifierParser.loadFromSheet(
            parse("{\"state-modifiers\": [{\"id\": \"g\", \"tiles\": [" + json + "]}]}"),
            registry,
            spriteIdOffset,
            sheetContext
        );
        return registry.getGroups().get(0).getTiles().values().iterator().next();
    }

    private static JsonValue parse(final String json) {
        return new JsonReader().parse(json);
    }
}
