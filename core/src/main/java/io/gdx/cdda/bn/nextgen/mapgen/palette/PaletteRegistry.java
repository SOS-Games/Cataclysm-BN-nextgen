package io.gdx.cdda.bn.nextgen.mapgen.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Discovered mapgen palettes keyed by id (P1). */
public final class PaletteRegistry {

    private final Map<String, MapgenPalette> palettesById = new LinkedHashMap<>();

    public void put(final MapgenPalette palette) {
        palettesById.put(palette.getId(), palette);
    }

    public Optional<MapgenPalette> find(final String id) {
        return Optional.ofNullable(palettesById.get(id));
    }

    public boolean contains(final String id) {
        return palettesById.containsKey(id);
    }

    public int size() {
        return palettesById.size();
    }

    public List<String> allIds() {
        final List<String> ids = new ArrayList<>(palettesById.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }

    public MergedCharMap merge(final List<String> paletteIds, final List<String> warnings) {
        final MergedCharMap merged = new MergedCharMap();
        if (paletteIds == null) {
            return merged;
        }
        for (final String paletteId : paletteIds) {
            if (paletteId == null || paletteId.isEmpty()) {
                continue;
            }
            final MapgenPalette palette = palettesById.get(paletteId);
            if (palette == null) {
                warnings.add("unknown palette: " + paletteId);
                continue;
            }
            merged.putAllTerrain(palette.getTerrainByCodePoint());
            merged.putAllFurniture(palette.getFurnitureByCodePoint());
        }
        return merged;
    }
}
