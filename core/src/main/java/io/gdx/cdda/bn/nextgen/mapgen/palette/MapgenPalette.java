package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Parsed BN {@code type: palette} char mappings (P1, P10). */
public final class MapgenPalette {

    private final String id;
    private final List<String> parentIds;
    private final PaletteCharMaps localCharMaps;

    public MapgenPalette(
        final String id,
        final List<String> parentIds,
        final Map<Integer, JsonValue> terrainByCodePoint,
        final Map<Integer, JsonValue> furnitureByCodePoint,
        final Map<Integer, JsonValue> itemsByCodePoint,
        final Map<Integer, JsonValue> monstersByCodePoint,
        final Map<Integer, JsonValue> monsterByCodePoint,
        final Map<Integer, Integer> translateByCodePoint
    ) {
        this(id, parentIds, terrainByCodePoint, furnitureByCodePoint, translateByCodePoint);
        if (itemsByCodePoint != null) {
            for (final Map.Entry<Integer, JsonValue> entry : itemsByCodePoint.entrySet()) {
                localCharMaps.putItemsNode(entry.getKey(), entry.getValue());
            }
        }
        if (monstersByCodePoint != null) {
            for (final Map.Entry<Integer, JsonValue> entry : monstersByCodePoint.entrySet()) {
                localCharMaps.putMonsterNode(entry.getKey(), entry.getValue());
            }
        }
        if (monsterByCodePoint != null) {
            for (final Map.Entry<Integer, JsonValue> entry : monsterByCodePoint.entrySet()) {
                localCharMaps.putMonsterNode(entry.getKey(), entry.getValue());
            }
        }
    }

    public MapgenPalette(
        final String id,
        final List<String> parentIds,
        final Map<Integer, JsonValue> terrainByCodePoint,
        final Map<Integer, JsonValue> furnitureByCodePoint,
        final Map<Integer, Integer> translateByCodePoint
    ) {
        this.id = id;
        this.parentIds = parentIds == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(parentIds));
        this.localCharMaps = new PaletteCharMaps();
        if (terrainByCodePoint != null) {
            for (final Map.Entry<Integer, JsonValue> entry : terrainByCodePoint.entrySet()) {
                localCharMaps.putTerrainNode(entry.getKey(), entry.getValue());
            }
        }
        if (furnitureByCodePoint != null) {
            for (final Map.Entry<Integer, JsonValue> entry : furnitureByCodePoint.entrySet()) {
                localCharMaps.putFurnitureNode(entry.getKey(), entry.getValue());
            }
        }
        if (translateByCodePoint != null) {
            for (final Map.Entry<Integer, Integer> entry : translateByCodePoint.entrySet()) {
                localCharMaps.putTranslate(entry.getKey(), entry.getValue());
            }
        }
    }

    /** Test helper — fixed string ids without inheritance. */
    public static MapgenPalette fromResolvedStrings(
        final String id,
        final Map<Integer, String> terrainByCodePoint,
        final Map<Integer, String> furnitureByCodePoint
    ) {
        final JsonReader reader = new JsonReader();
        final Map<Integer, JsonValue> terrain = new HashMap<>();
        final Map<Integer, JsonValue> furniture = new HashMap<>();
        if (terrainByCodePoint != null) {
            for (final Map.Entry<Integer, String> entry : terrainByCodePoint.entrySet()) {
                terrain.put(entry.getKey(), reader.parse('"' + entry.getValue() + '"'));
            }
        }
        if (furnitureByCodePoint != null) {
            for (final Map.Entry<Integer, String> entry : furnitureByCodePoint.entrySet()) {
                furniture.put(entry.getKey(), reader.parse('"' + entry.getValue() + '"'));
            }
        }
        return new MapgenPalette(id, List.of(), terrain, furniture, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public String getId() {
        return id;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    PaletteCharMaps getLocalCharMaps() {
        return localCharMaps;
    }

    public Optional<String> terrainForCodePoint(final int codePoint) {
        return Optional.ofNullable(localCharMaps.getTerrainByCodePoint().get(codePoint))
            .flatMap(PaletteCharResolver::resolveId);
    }

    public Optional<String> furnitureForCodePoint(final int codePoint) {
        return Optional.ofNullable(localCharMaps.getFurnitureByCodePoint().get(codePoint))
            .flatMap(PaletteCharResolver::resolveId);
    }

    public Map<Integer, JsonValue> getTerrainByCodePoint() {
        return Collections.unmodifiableMap(localCharMaps.getTerrainByCodePoint());
    }

    public Map<Integer, JsonValue> getFurnitureByCodePoint() {
        return Collections.unmodifiableMap(localCharMaps.getFurnitureByCodePoint());
    }
}
