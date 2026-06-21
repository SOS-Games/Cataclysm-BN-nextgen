package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonValue;

import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Merged per-char item/monster format placings from palettes and inline mapgen overrides. */
public final class MergedFormatPlacings {

    private final Map<Integer, JsonValue> itemsByCodePoint = new HashMap<>();
    private final Map<Integer, JsonValue> monsterByCodePoint = new HashMap<>();

    public void mergeFrom(final PaletteCharMaps source) {
        if (source == null) {
            return;
        }
        itemsByCodePoint.putAll(source.getItemsByCodePoint());
        monsterByCodePoint.putAll(source.getMonsterByCodePoint());
    }

    public void putInlineItems(final Map<Integer, JsonValue> nodes) {
        if (nodes != null) {
            itemsByCodePoint.putAll(nodes);
        }
    }

    public void putInlineMonster(final Map<Integer, JsonValue> nodes) {
        if (nodes != null) {
            monsterByCodePoint.putAll(nodes);
        }
    }

    public void applyTranslate(final Map<Integer, Integer> translate) {
        if (translate == null || translate.isEmpty()) {
            return;
        }
        applyTranslate(itemsByCodePoint, translate);
        applyTranslate(monsterByCodePoint, translate);
    }

    public Map<Integer, JsonValue> getItemsByCodePoint() {
        return itemsByCodePoint;
    }

    public Map<Integer, JsonValue> getMonsterByCodePoint() {
        return monsterByCodePoint;
    }

    public static MergedFormatPlacings merge(
        final PaletteRegistry palettes,
        final List<String> paletteIds,
        final JsonValue object,
        final List<String> warnings
    ) {
        final MergedFormatPlacings merged = new MergedFormatPlacings();
        final PaletteCharMaps paletteNodes = new PaletteCharMaps();
        if (paletteIds != null) {
            for (final String paletteId : paletteIds) {
                if (paletteId == null || paletteId.isEmpty()) {
                    continue;
                }
                paletteNodes.mergeFrom(PaletteResolver.resolveWithParents(palettes, paletteId, warnings));
            }
        }
        merged.mergeFrom(paletteNodes);
        merged.putInlineItems(PaletteParser.parseCharSectionNodes(object == null ? null : object.get("items")));
        merged.putInlineMonster(PaletteParser.parseCharSectionNodes(object == null ? null : object.get("monster")));
        merged.putInlineMonster(PaletteParser.parseCharSectionNodes(object == null ? null : object.get("monsters")));
        merged.applyTranslate(paletteNodes.getTranslateByCodePoint());
        return merged;
    }

    private static void applyTranslate(
        final Map<Integer, JsonValue> nodes,
        final Map<Integer, Integer> translate
    ) {
        for (final Map.Entry<Integer, Integer> entry : translate.entrySet()) {
            final JsonValue target = nodes.get(entry.getValue());
            if (target != null) {
                nodes.put(entry.getKey(), target);
            }
        }
    }
}
