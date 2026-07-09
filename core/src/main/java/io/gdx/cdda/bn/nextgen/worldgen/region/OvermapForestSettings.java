package io.gdx.cdda.bn.nextgen.worldgen.region;

/** Forest noise thresholds from {@code overmap_forest_settings} (W9). */
public final class OvermapForestSettings {

    private final double noiseThresholdForest;
    private final double noiseThresholdForestThick;
    private final String forestOter;
    private final String forestThickOter;
    private final int riverFloodplainBufferDistanceMin;
    private final int riverFloodplainBufferDistanceMax;

    public OvermapForestSettings(
        final double noiseThresholdForest,
        final double noiseThresholdForestThick,
        final String forestOter,
        final String forestThickOter
    ) {
        this(noiseThresholdForest, noiseThresholdForestThick, forestOter, forestThickOter, 3, 15);
    }

    public OvermapForestSettings(
        final double noiseThresholdForest,
        final double noiseThresholdForestThick,
        final String forestOter,
        final String forestThickOter,
        final int riverFloodplainBufferDistanceMin,
        final int riverFloodplainBufferDistanceMax
    ) {
        this.noiseThresholdForest = noiseThresholdForest;
        this.noiseThresholdForestThick = noiseThresholdForestThick;
        this.forestOter = forestOter == null || forestOter.isEmpty() ? "forest" : forestOter;
        this.forestThickOter = forestThickOter == null || forestThickOter.isEmpty()
            ? "forest_thick"
            : forestThickOter;
        this.riverFloodplainBufferDistanceMin = Math.max(0, riverFloodplainBufferDistanceMin);
        this.riverFloodplainBufferDistanceMax = Math.max(
            this.riverFloodplainBufferDistanceMin,
            riverFloodplainBufferDistanceMax
        );
    }

    public static OvermapForestSettings defaults() {
        return new OvermapForestSettings(0.35, 0.0, "forest", "forest_thick", 3, 15);
    }

    public double getNoiseThresholdForest() {
        return noiseThresholdForest;
    }

    public double getNoiseThresholdForestThick() {
        return noiseThresholdForestThick;
    }

    public boolean hasThickForest() {
        return noiseThresholdForestThick > noiseThresholdForest;
    }

    public String getForestOter() {
        return forestOter;
    }

    public String getForestThickOter() {
        return forestThickOter;
    }

    public int getRiverFloodplainBufferDistanceMin() {
        return riverFloodplainBufferDistanceMin;
    }

    public int getRiverFloodplainBufferDistanceMax() {
        return riverFloodplainBufferDistanceMax;
    }
}
