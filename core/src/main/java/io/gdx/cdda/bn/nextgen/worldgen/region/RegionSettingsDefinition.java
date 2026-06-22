package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parsed overmap-relevant {@code region_settings} entry (W9). */
public final class RegionSettingsDefinition {

    private final String id;
    private final String defaultOter;
    private final OvermapForestSettings forestSettings;
    private final OvermapLakeSettings lakeSettings;
    private final Map<String, Integer> cityHouseWeights;

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final OvermapForestSettings forestSettings,
        final Map<String, Integer> cityHouseWeights
    ) {
        this(id, defaultOter, forestSettings, OvermapLakeSettings.disabled(), cityHouseWeights);
    }

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final OvermapForestSettings forestSettings,
        final OvermapLakeSettings lakeSettings,
        final Map<String, Integer> cityHouseWeights
    ) {
        this.id = id == null ? "" : id;
        this.defaultOter = defaultOter == null || defaultOter.isEmpty() ? "field" : defaultOter;
        this.forestSettings = forestSettings == null ? OvermapForestSettings.defaults() : forestSettings;
        this.lakeSettings = lakeSettings == null ? OvermapLakeSettings.disabled() : lakeSettings;
        if (cityHouseWeights == null || cityHouseWeights.isEmpty()) {
            this.cityHouseWeights = Collections.emptyMap();
        } else {
            this.cityHouseWeights = Collections.unmodifiableMap(new LinkedHashMap<>(cityHouseWeights));
        }
    }

    public String getId() {
        return id;
    }

    public String getDefaultOter() {
        return defaultOter;
    }

    public OvermapForestSettings getForestSettings() {
        return forestSettings;
    }

    public OvermapLakeSettings getLakeSettings() {
        return lakeSettings;
    }

    public Map<String, Integer> getCityHouseWeights() {
        return cityHouseWeights;
    }

    public boolean hasCityHouseWeights() {
        return !cityHouseWeights.isEmpty();
    }
}
