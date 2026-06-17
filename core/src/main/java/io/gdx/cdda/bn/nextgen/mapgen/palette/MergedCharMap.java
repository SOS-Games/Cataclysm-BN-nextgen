package io.gdx.cdda.bn.nextgen.mapgen.palette;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Merged terrain/furniture char maps from one or more palette ids (P1). */
public final class MergedCharMap {

    private final Map<Integer, String> terrainByCodePoint = new HashMap<>();
    private final Map<Integer, String> furnitureByCodePoint = new HashMap<>();

    public Optional<String> terrainForCodePoint(final int codePoint) {
        return Optional.ofNullable(terrainByCodePoint.get(codePoint));
    }

    public Optional<String> furnitureForCodePoint(final int codePoint) {
        return Optional.ofNullable(furnitureByCodePoint.get(codePoint));
    }

    public void putTerrain(final int codePoint, final String terrainId) {
        if (terrainId != null && !terrainId.isEmpty()) {
            terrainByCodePoint.put(codePoint, terrainId);
        }
    }

    public void putFurniture(final int codePoint, final String furnitureId) {
        if (furnitureId != null && !furnitureId.isEmpty()) {
            furnitureByCodePoint.put(codePoint, furnitureId);
        }
    }

    public void putAllTerrain(final Map<Integer, String> terrain) {
        for (final Map.Entry<Integer, String> entry : terrain.entrySet()) {
            putTerrain(entry.getKey(), entry.getValue());
        }
    }

    public void putAllFurniture(final Map<Integer, String> furniture) {
        for (final Map.Entry<Integer, String> entry : furniture.entrySet()) {
            putFurniture(entry.getKey(), entry.getValue());
        }
    }
}
