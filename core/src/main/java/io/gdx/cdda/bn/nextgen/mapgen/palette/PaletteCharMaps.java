package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;

/** Mutable terrain/furniture JSON nodes plus symbol translate (P10). */
final class PaletteCharMaps {

    private final Map<Integer, JsonValue> terrainByCodePoint = new HashMap<>();
    private final Map<Integer, JsonValue> furnitureByCodePoint = new HashMap<>();
    private final Map<Integer, Integer> translateByCodePoint = new HashMap<>();

    void mergeFrom(final PaletteCharMaps other) {
        if (other == null) {
            return;
        }
        terrainByCodePoint.putAll(other.terrainByCodePoint);
        furnitureByCodePoint.putAll(other.furnitureByCodePoint);
        translateByCodePoint.putAll(other.translateByCodePoint);
    }

    void putTerrainNode(final int codePoint, final JsonValue node) {
        if (node != null) {
            terrainByCodePoint.put(codePoint, node);
        }
    }

    void putFurnitureNode(final int codePoint, final JsonValue node) {
        if (node != null) {
            furnitureByCodePoint.put(codePoint, node);
        }
    }

    void putTranslate(final int aliasCodePoint, final int targetCodePoint) {
        translateByCodePoint.put(aliasCodePoint, targetCodePoint);
    }

    Map<Integer, JsonValue> getTerrainByCodePoint() {
        return terrainByCodePoint;
    }

    Map<Integer, JsonValue> getFurnitureByCodePoint() {
        return furnitureByCodePoint;
    }

    Map<Integer, Integer> getTranslateByCodePoint() {
        return translateByCodePoint;
    }
}
