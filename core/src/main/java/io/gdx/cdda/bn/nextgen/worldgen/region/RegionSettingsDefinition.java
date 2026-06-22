package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parsed overmap-relevant {@code region_settings} entry (W9, W14). */
public final class RegionSettingsDefinition {

    private final String id;
    private final String defaultOter;
    private final OvermapForestSettings forestSettings;
    private final OvermapLakeSettings lakeSettings;
    private final Map<String, Integer> cityHouseWeights;
    private final CitySizeSettings citySizeSettings;
    private final OvermapSpecialSettings specialSettings;
    private final OvermapTerrainSettings terrainSettings;

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final OvermapForestSettings forestSettings,
        final Map<String, Integer> cityHouseWeights
    ) {
        this(
            id,
            defaultOter,
            forestSettings,
            OvermapLakeSettings.disabled(),
            cityHouseWeights,
            CitySizeSettings.disabled(),
            OvermapSpecialSettings.disabled(),
            OvermapTerrainSettings.disabled()
        );
    }

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final OvermapForestSettings forestSettings,
        final OvermapLakeSettings lakeSettings,
        final Map<String, Integer> cityHouseWeights
    ) {
        this(
            id,
            defaultOter,
            forestSettings,
            lakeSettings,
            cityHouseWeights,
            CitySizeSettings.disabled(),
            OvermapSpecialSettings.disabled(),
            OvermapTerrainSettings.disabled()
        );
    }

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final OvermapForestSettings forestSettings,
        final OvermapLakeSettings lakeSettings,
        final Map<String, Integer> cityHouseWeights,
        final CitySizeSettings citySizeSettings,
        final OvermapSpecialSettings specialSettings,
        final OvermapTerrainSettings terrainSettings
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
        this.citySizeSettings = citySizeSettings == null ? CitySizeSettings.disabled() : citySizeSettings;
        this.specialSettings = specialSettings == null ? OvermapSpecialSettings.disabled() : specialSettings;
        this.terrainSettings = terrainSettings == null ? OvermapTerrainSettings.disabled() : terrainSettings;
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

    public CitySizeSettings getCitySizeSettings() {
        return citySizeSettings;
    }

    public OvermapSpecialSettings getSpecialSettings() {
        return specialSettings;
    }

    public OvermapTerrainSettings getTerrainSettings() {
        return terrainSettings;
    }
}
