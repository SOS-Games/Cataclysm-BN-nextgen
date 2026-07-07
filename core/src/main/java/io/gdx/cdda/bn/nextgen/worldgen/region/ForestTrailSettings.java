package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** {@code forest_trail_settings} from BN {@code region_settings} (W17e). */
public final class ForestTrailSettings {

    private final int chance;
    private final int borderPointChance;
    private final int minimumForestSize;
    private final int randomPointMin;
    private final int randomPointMax;
    private final int randomPointSizeScalar;
    private final int trailheadChance;
    private final int trailheadRoadDistance;
    private final Map<String, Integer> trailheads;

    public ForestTrailSettings(
        final int chance,
        final int borderPointChance,
        final int minimumForestSize,
        final int randomPointMin,
        final int randomPointMax,
        final int randomPointSizeScalar,
        final int trailheadChance,
        final int trailheadRoadDistance,
        final Map<String, Integer> trailheads
    ) {
        this.chance = Math.max(0, chance);
        this.borderPointChance = Math.max(1, borderPointChance);
        this.minimumForestSize = Math.max(1, minimumForestSize);
        this.randomPointMin = Math.max(0, randomPointMin);
        this.randomPointMax = Math.max(this.randomPointMin, randomPointMax);
        this.randomPointSizeScalar = Math.max(1, randomPointSizeScalar);
        this.trailheadChance = Math.max(1, trailheadChance);
        this.trailheadRoadDistance = Math.max(1, trailheadRoadDistance);
        this.trailheads = trailheads == null || trailheads.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(trailheads));
    }

    public static ForestTrailSettings disabled() {
        return new ForestTrailSettings(0, 2, 50, 4, 50, 100, 1, 6, Collections.emptyMap());
    }

    public boolean isEnabled() {
        return chance > 0;
    }

    public int getChance() {
        return chance;
    }

    public int getBorderPointChance() {
        return borderPointChance;
    }

    public int getMinimumForestSize() {
        return minimumForestSize;
    }

    public int getRandomPointMin() {
        return randomPointMin;
    }

    public int getRandomPointMax() {
        return randomPointMax;
    }

    public int getRandomPointSizeScalar() {
        return randomPointSizeScalar;
    }

    public int getTrailheadChance() {
        return trailheadChance;
    }

    public int getTrailheadRoadDistance() {
        return trailheadRoadDistance;
    }

    public Map<String, Integer> getTrailheads() {
        return trailheads;
    }
}
