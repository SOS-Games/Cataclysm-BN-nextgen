package io.gdx.cdda.bn.nextgen.tileset.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.model.StateModifierGroup;
import io.gdx.cdda.bn.nextgen.tileset.model.StateModifierRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.StateModifierTile;
import io.gdx.cdda.bn.nextgen.tileset.parse.TileRegistrar.SheetContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses {@code state-modifiers} on a {@code tiles-new} sheet (unit 07d). */
public final class StateModifierParser {

    private StateModifierParser() {}

    public static void loadFromSheet(
        final JsonValue sheet,
        final StateModifierRegistry registry,
        final int spriteIdOffset,
        final SheetContext sheetContext
    ) throws IOException {
        if (!sheet.has("state-modifiers")) {
            return;
        }
        final JsonValue modifiers = sheet.get("state-modifiers");
        if (modifiers == null || !modifiers.isArray()) {
            return;
        }
        for (JsonValue modGroup = modifiers.child; modGroup != null; modGroup = modGroup.next) {
            registry.merge(parseGroup(modGroup, spriteIdOffset, sheetContext));
        }
    }

    private static StateModifierGroup parseGroup(
        final JsonValue modGroup,
        final int spriteIdOffset,
        final SheetContext sheetContext
    ) throws IOException {
        final String groupId = modGroup.getString("id", "");
        final boolean overrideLower = modGroup.getBoolean("override", false);
        final boolean useOffsetMode = modGroup.getBoolean("use_offset", true);
        final List<String> whitelist = readStringArray(modGroup, "whitelist");
        final List<String> blacklist = readStringArray(modGroup, "blacklist");

        if (!modGroup.has("tiles")) {
            throw new IOException("state-modifier group must have a 'tiles' array");
        }
        final JsonValue tilesArray = modGroup.get("tiles");
        if (tilesArray == null || !tilesArray.isArray()) {
            throw new IOException("state-modifier group must have a 'tiles' array");
        }

        final Map<String, StateModifierTile> tiles = new LinkedHashMap<>();
        for (JsonValue tileEntry = tilesArray.child; tileEntry != null; tileEntry = tileEntry.next) {
            final StateModifierTile tile = parseTile(tileEntry, spriteIdOffset, sheetContext);
            tiles.put(tile.getStateId(), tile);
        }

        return new StateModifierGroup(
            groupId,
            overrideLower,
            useOffsetMode,
            normalizeFilterList(whitelist),
            normalizeFilterList(blacklist),
            tiles
        );
    }

    private static StateModifierTile parseTile(
        final JsonValue tileEntry,
        final int spriteIdOffset,
        final SheetContext sheetContext
    ) throws IOException {
        final String stateId = tileEntry.getString("id", "");
        if (stateId.isEmpty()) {
            throw new IOException("state-modifier tile missing required \"id\"");
        }
        final Integer foregroundSprite = resolveForegroundSprite(tileEntry, spriteIdOffset);
        final int offsetX = tileEntry.getInt("offset_x", sheetContext.getOffsetX());
        final int offsetY = tileEntry.getInt("offset_y", sheetContext.getOffsetY());
        return new StateModifierTile(stateId, foregroundSprite, offsetX, offsetY);
    }

    static Integer resolveForegroundSprite(final JsonValue tileEntry, final int spriteIdOffset) {
        if (!tileEntry.has("fg")) {
            return null;
        }
        final JsonValue foreground = tileEntry.get("fg");
        if (foreground == null || foreground.isNull()) {
            return null;
        }
        if (!foreground.isNumber()) {
            return null;
        }
        final int value = foreground.asInt();
        if (value < 0) {
            return null;
        }
        return value + spriteIdOffset;
    }

    private static List<String> readStringArray(final JsonValue object, final String fieldName) {
        final List<String> values = new ArrayList<>();
        if (!object.has(fieldName)) {
            return values;
        }
        final JsonValue array = object.get(fieldName);
        if (array == null || !array.isArray()) {
            return values;
        }
        for (JsonValue child = array.child; child != null; child = child.next) {
            if (child.isString()) {
                values.add(child.asString());
            }
        }
        return values;
    }

    static List<String> normalizeFilterList(final List<String> values) {
        final List<String> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        final List<String> normalized = new ArrayList<>(sorted.size());
        String previous = null;
        for (final String value : sorted) {
            if (!value.equals(previous)) {
                normalized.add(value);
                previous = value;
            }
        }
        return normalized;
    }
}
