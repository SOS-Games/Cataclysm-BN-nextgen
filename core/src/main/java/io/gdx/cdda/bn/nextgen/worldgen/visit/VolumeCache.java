package io.gdx.cdda.bn.nextgen.worldgen.visit;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** LRU cache of {@link MapgenPreviewService.MapgenBuildingResult} per placed building (W7). */
public final class VolumeCache {

    private final int maxEntries;
    private final Map<VolumeCacheKey, MapgenPreviewService.MapgenBuildingResult> entries;

    public VolumeCache(final int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
        this.entries = new LinkedHashMap<VolumeCacheKey, MapgenPreviewService.MapgenBuildingResult>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(
                final Map.Entry<VolumeCacheKey, MapgenPreviewService.MapgenBuildingResult> eldest
            ) {
                return size() > VolumeCache.this.maxEntries;
            }
        };
    }

    public MapgenPreviewService.MapgenBuildingResult getOrBuild(
        final VolumeCacheKey key,
        final Supplier<MapgenPreviewService.MapgenBuildingResult> builder
    ) {
        final MapgenPreviewService.MapgenBuildingResult cached = entries.get(key);
        if (cached != null) {
            return cached;
        }
        final MapgenPreviewService.MapgenBuildingResult built = builder.get();
        entries.put(key, built);
        return built;
    }

    public Optional<MapgenPreviewService.MapgenBuildingResult> get(final VolumeCacheKey key) {
        return Optional.ofNullable(entries.get(key));
    }

    public boolean contains(final VolumeCacheKey key) {
        return entries.containsKey(key);
    }

    public void clear() {
        entries.clear();
    }

    public int getMaxEntries() {
        return maxEntries;
    }
}
