package io.gdx.cdda.bn.nextgen.worldgen;

/** BN {@code WORLD_OPTIONS} defaults used when region JSON omits values (W17a). */
public final class WorldgenWorldOptions {

    /** Matches BN {@code city_settings} sentinel — use world option instead. */
    public static final int USE_REGION_DEFAULT = -1;

    private final int citySize;
    private final int citySpacing;

    public WorldgenWorldOptions(final int citySize, final int citySpacing) {
        this.citySize = Math.max(0, citySize);
        this.citySpacing = Math.max(0, citySpacing);
    }

    /** BN new-world defaults: {@code CITY_SIZE}=8, {@code CITY_SPACING}=4. */
    public static WorldgenWorldOptions bnDefaults() {
        return new WorldgenWorldOptions(8, 4);
    }

    /** Innawoods-style preset: no cities or inter-city roads. */
    public static WorldgenWorldOptions noCities() {
        return new WorldgenWorldOptions(0, 0);
    }

    public int getCitySize() {
        return citySize;
    }

    public int getCitySpacing() {
        return citySpacing;
    }

    public WorldgenWorldOptions withCitySize(final int citySize) {
        return new WorldgenWorldOptions(citySize, citySpacing);
    }

    public WorldgenWorldOptions withCitySpacing(final int citySpacing) {
        return new WorldgenWorldOptions(citySize, citySpacing);
    }
}
