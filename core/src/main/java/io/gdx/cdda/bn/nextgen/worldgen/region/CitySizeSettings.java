package io.gdx.cdda.bn.nextgen.worldgen.region;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenWorldOptions;

/** Urban blob size and spacing from region {@code city} block (W14b). */
public final class CitySizeSettings {

    /** BN sentinel: inherit {@link WorldgenWorldOptions#getCitySize()}. */
    public static final int USE_WORLD_OPTION = WorldgenWorldOptions.USE_REGION_DEFAULT;

    private final int citySize;
    private final int citySpacing;
    private final boolean cityIsolated;

    public CitySizeSettings(final int citySize, final int citySpacing, final boolean cityIsolated) {
        this.citySize = citySize;
        this.citySpacing = citySpacing;
        this.cityIsolated = cityIsolated;
    }

    public static CitySizeSettings disabled() {
        return new CitySizeSettings(0, 0, false);
    }

    public boolean isEnabled() {
        return citySize > 0 || citySpacing > 0 || cityIsolated;
    }

    public boolean isEnabled(final WorldgenWorldOptions worldOptions) {
        return resolve(worldOptions).isEnabled();
    }

    public CitySizeSettings resolve(final WorldgenWorldOptions worldOptions) {
        if (worldOptions == null) {
            return withResolvedSize(Math.max(0, citySize), Math.max(0, citySpacing));
        }
        final int resolvedSize = citySize == USE_WORLD_OPTION ? worldOptions.getCitySize() : Math.max(0, citySize);
        final int resolvedSpacing = citySpacing == USE_WORLD_OPTION
            ? worldOptions.getCitySpacing()
            : Math.max(0, citySpacing);
        return withResolvedSize(resolvedSize, resolvedSpacing);
    }

    private CitySizeSettings withResolvedSize(final int resolvedSize, final int resolvedSpacing) {
        return new CitySizeSettings(resolvedSize, resolvedSpacing, cityIsolated);
    }

    public int getCitySize() {
        return citySize == USE_WORLD_OPTION ? 0 : Math.max(0, citySize);
    }

    public int getRawCitySize() {
        return citySize;
    }

    public int getCitySpacing() {
        return citySpacing == USE_WORLD_OPTION ? 0 : Math.max(0, citySpacing);
    }

    public int getRawCitySpacing() {
        return citySpacing;
    }

    public boolean isCityIsolated() {
        return cityIsolated;
    }
}
