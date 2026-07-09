package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.Collections;
import java.util.Map;

/** Parsed overmap-relevant {@code region_settings} entry (W9, W14). */
public final class RegionSettingsDefinition {

    private final String id;
    private final String defaultOter;
    private final String displayOter;
    private final RegionGroundcoverSettings defaultGroundcover;
    private final OvermapForestSettings forestSettings;
    private final OvermapLakeSettings lakeSettings;
    private final CityContentWeights cityContentWeights;
    private final CitySizeSettings citySizeSettings;
    private final OvermapSpecialSettings specialSettings;
    private final OvermapTerrainSettings terrainSettings;
    private final ForestTrailSettings forestTrailSettings;
    private final UndergroundNetworkSettings undergroundNetworkSettings;
    private final double riverScale;

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
            CityContentWeights.housesOnly(cityHouseWeights),
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
            CityContentWeights.housesOnly(cityHouseWeights),
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
        final CityContentWeights cityContentWeights,
        final CitySizeSettings citySizeSettings,
        final OvermapSpecialSettings specialSettings,
        final OvermapTerrainSettings terrainSettings
    ) {
        this(id, defaultOter, forestSettings, lakeSettings, cityContentWeights, citySizeSettings, specialSettings,
            terrainSettings, ForestTrailSettings.disabled());
    }

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final OvermapForestSettings forestSettings,
        final OvermapLakeSettings lakeSettings,
        final CityContentWeights cityContentWeights,
        final CitySizeSettings citySizeSettings,
        final OvermapSpecialSettings specialSettings,
        final OvermapTerrainSettings terrainSettings,
        final ForestTrailSettings forestTrailSettings
    ) {
        this(id, defaultOter, forestSettings, lakeSettings, cityContentWeights, citySizeSettings, specialSettings,
            terrainSettings, forestTrailSettings, UndergroundNetworkSettings.disabled());
    }

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final OvermapForestSettings forestSettings,
        final OvermapLakeSettings lakeSettings,
        final CityContentWeights cityContentWeights,
        final CitySizeSettings citySizeSettings,
        final OvermapSpecialSettings specialSettings,
        final OvermapTerrainSettings terrainSettings,
        final ForestTrailSettings forestTrailSettings,
        final UndergroundNetworkSettings undergroundNetworkSettings
    ) {
        this(id, defaultOter, "", RegionGroundcoverSettings.defaults(), forestSettings, lakeSettings, cityContentWeights,
            citySizeSettings, specialSettings, terrainSettings, forestTrailSettings, undergroundNetworkSettings, 4.0);
    }

    public RegionSettingsDefinition(
        final String id,
        final String defaultOter,
        final String displayOter,
        final RegionGroundcoverSettings defaultGroundcover,
        final OvermapForestSettings forestSettings,
        final OvermapLakeSettings lakeSettings,
        final CityContentWeights cityContentWeights,
        final CitySizeSettings citySizeSettings,
        final OvermapSpecialSettings specialSettings,
        final OvermapTerrainSettings terrainSettings,
        final ForestTrailSettings forestTrailSettings,
        final UndergroundNetworkSettings undergroundNetworkSettings,
        final double riverScale
    ) {
        this.id = id == null ? "" : id;
        this.defaultOter = defaultOter == null || defaultOter.isEmpty() ? "field" : defaultOter;
        this.displayOter = displayOter == null ? "" : displayOter;
        this.defaultGroundcover = defaultGroundcover == null
            ? RegionGroundcoverSettings.defaults()
            : defaultGroundcover;
        this.forestSettings = forestSettings == null ? OvermapForestSettings.defaults() : forestSettings;
        this.lakeSettings = lakeSettings == null ? OvermapLakeSettings.disabled() : lakeSettings;
        this.cityContentWeights = cityContentWeights == null
            ? CityContentWeights.empty()
            : cityContentWeights;
        this.citySizeSettings = citySizeSettings == null ? CitySizeSettings.disabled() : citySizeSettings;
        this.specialSettings = specialSettings == null ? OvermapSpecialSettings.disabled() : specialSettings;
        this.terrainSettings = terrainSettings == null ? OvermapTerrainSettings.disabled() : terrainSettings;
        this.forestTrailSettings = forestTrailSettings == null
            ? ForestTrailSettings.disabled()
            : forestTrailSettings;
        this.undergroundNetworkSettings = undergroundNetworkSettings == null
            ? UndergroundNetworkSettings.disabled()
            : undergroundNetworkSettings;
        this.riverScale = riverScale;
    }

    /** Compatibility constructor for callers passing house weights only. */
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
        this(
            id,
            defaultOter,
            forestSettings,
            lakeSettings,
            CityContentWeights.housesOnly(cityHouseWeights),
            citySizeSettings,
            specialSettings,
            terrainSettings
        );
    }

    public String getId() {
        return id;
    }

    public String getDefaultOter() {
        return defaultOter;
    }

    public String getDisplayOter() {
        return displayOter;
    }

    public boolean hasDisplayOter() {
        return displayOter != null && !displayOter.isEmpty();
    }

    public RegionGroundcoverSettings getDefaultGroundcover() {
        return defaultGroundcover;
    }

    public String getDefaultGroundcoverTerrainId() {
        return defaultGroundcover.getDefaultTerrainId();
    }

    public OvermapForestSettings getForestSettings() {
        return forestSettings;
    }

    public OvermapLakeSettings getLakeSettings() {
        return lakeSettings;
    }

    public CityContentWeights getCityContentWeights() {
        return cityContentWeights;
    }

    public Map<String, Integer> getCityHouseWeights() {
        return cityContentWeights.getHouses();
    }

    public boolean hasCityHouseWeights() {
        return cityContentWeights.hasHouseWeights();
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

    public ForestTrailSettings getForestTrailSettings() {
        return forestTrailSettings;
    }

    public UndergroundNetworkSettings getUndergroundNetworkSettings() {
        return undergroundNetworkSettings;
    }

    public double getRiverScale() {
        return riverScale;
    }

    public boolean isRiverGenerationEnabled() {
        return riverScale > 0.0;
    }
}
