package io.gdx.cdda.bn.nextgen.mapgen.palette;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Parsed BN {@code type: palette} char mappings (P1). */
public final class MapgenPalette {

    private final String id;
    private final Map<Integer, String> terrainByCodePoint;
    private final Map<Integer, String> furnitureByCodePoint;

    public MapgenPalette(
        final String id,
        final Map<Integer, String> terrainByCodePoint,
        final Map<Integer, String> furnitureByCodePoint
    ) {
        this.id = id;
        this.terrainByCodePoint = Collections.unmodifiableMap(new HashMap<>(terrainByCodePoint));
        this.furnitureByCodePoint = Collections.unmodifiableMap(new HashMap<>(furnitureByCodePoint));
    }

    public String getId() {
        return id;
    }

    public Optional<String> terrainForCodePoint(final int codePoint) {
        return Optional.ofNullable(terrainByCodePoint.get(codePoint));
    }

    public Optional<String> furnitureForCodePoint(final int codePoint) {
        return Optional.ofNullable(furnitureByCodePoint.get(codePoint));
    }

    public Map<Integer, String> getTerrainByCodePoint() {
        return terrainByCodePoint;
    }

    public Map<Integer, String> getFurnitureByCodePoint() {
        return furnitureByCodePoint;
    }
}
