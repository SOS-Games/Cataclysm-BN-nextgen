package io.gdx.cdda.bn.nextgen.worldgen.region;

/** Swamp / beach thresholds from region terrain settings (W14c). */
public final class OvermapTerrainSettings {

    private final boolean enabled;
    private final double noiseThresholdSwampAdjacentWater;
    private final double noiseThresholdSwampIsolated;
    private final String swampOter;
    private final String beachOter;

    public OvermapTerrainSettings(
        final boolean enabled,
        final double noiseThresholdSwampAdjacentWater,
        final double noiseThresholdSwampIsolated,
        final String swampOter,
        final String beachOter
    ) {
        this.enabled = enabled;
        this.noiseThresholdSwampAdjacentWater = noiseThresholdSwampAdjacentWater;
        this.noiseThresholdSwampIsolated = noiseThresholdSwampIsolated;
        this.swampOter = swampOter == null || swampOter.isEmpty() ? "forest_water" : swampOter;
        this.beachOter = beachOter == null || beachOter.isEmpty() ? "beach" : beachOter;
    }

    public static OvermapTerrainSettings disabled() {
        return new OvermapTerrainSettings(false, 0.0, 0.0, "forest_water", "beach");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasSwampPass() {
        return enabled
            && (noiseThresholdSwampAdjacentWater > 0.0 || noiseThresholdSwampIsolated > 0.0);
    }

    public boolean hasBeachPass() {
        return enabled;
    }

    public double getNoiseThresholdSwampAdjacentWater() {
        return noiseThresholdSwampAdjacentWater;
    }

    public double getNoiseThresholdSwampIsolated() {
        return noiseThresholdSwampIsolated;
    }

    public String getSwampOter() {
        return swampOter;
    }

    public String getBeachOter() {
        return beachOter;
    }
}
