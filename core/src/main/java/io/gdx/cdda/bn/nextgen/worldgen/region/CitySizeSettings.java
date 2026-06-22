package io.gdx.cdda.bn.nextgen.worldgen.region;

/** Urban blob size and spacing from region {@code city} block (W14b). */
public final class CitySizeSettings {

    private final int citySize;
    private final int citySpacing;
    private final boolean cityIsolated;

    public CitySizeSettings(final int citySize, final int citySpacing, final boolean cityIsolated) {
        this.citySize = Math.max(0, citySize);
        this.citySpacing = Math.max(0, citySpacing);
        this.cityIsolated = cityIsolated;
    }

    public static CitySizeSettings disabled() {
        return new CitySizeSettings(0, 0, false);
    }

    public boolean isEnabled() {
        return citySize > 0 || citySpacing > 0 || cityIsolated;
    }

    public int getCitySize() {
        return citySize;
    }

    public int getCitySpacing() {
        return citySpacing;
    }

    public boolean isCityIsolated() {
        return cityIsolated;
    }
}
