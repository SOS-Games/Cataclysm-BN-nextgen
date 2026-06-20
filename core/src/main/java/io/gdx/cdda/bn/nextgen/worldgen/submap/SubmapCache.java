package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** LRU cache for generated submaps keyed by world seed + OMT coord (W3). */
public final class SubmapCache {

    private final int maxEntries;
    private final LinkedHashMap<SubmapKey, MapGrid> entries;

    public SubmapCache(final int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<SubmapKey, MapGrid>(maxEntries + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<SubmapKey, MapGrid> eldest) {
                return size() > SubmapCache.this.maxEntries;
            }
        };
    }

    public Optional<MapGrid> get(final SubmapKey key) {
        return Optional.ofNullable(entries.get(key));
    }

    public void put(final SubmapKey key, final MapGrid grid) {
        if (key == null || grid == null) {
            return;
        }
        entries.put(key, grid);
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
