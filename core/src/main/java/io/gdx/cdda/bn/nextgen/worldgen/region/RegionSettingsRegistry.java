package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Index of {@link RegionSettingsDefinition} by region id (W9). */
public final class RegionSettingsRegistry {

    private final Map<String, RegionSettingsDefinition> byId;

    public RegionSettingsRegistry(final Map<String, RegionSettingsDefinition> byId) {
        this.byId = byId == null || byId.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(byId));
    }

    public static RegionSettingsRegistry empty() {
        return new RegionSettingsRegistry(Collections.emptyMap());
    }

    public Optional<RegionSettingsDefinition> find(final String regionId) {
        if (regionId == null || regionId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(regionId));
    }

    public Optional<RegionSettingsDefinition> resolve(final String regionId) {
        final Optional<RegionSettingsDefinition> direct = find(regionId);
        if (direct.isPresent()) {
            return direct;
        }
        if (!"default".equals(regionId)) {
            return find("default");
        }
        return Optional.empty();
    }

    public List<String> regionIds() {
        final List<String> ids = new ArrayList<>(byId.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }

    public int size() {
        return byId.size();
    }
}
