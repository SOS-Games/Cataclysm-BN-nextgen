package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Weighted {@code overmap_special} placement from region JSON (W14a). */
public final class OvermapSpecialSettings {

    private final Map<String, Integer> weightedSpecials;
    private final int minCount;
    private final int maxCount;

    public OvermapSpecialSettings(
        final Map<String, Integer> weightedSpecials,
        final int minCount,
        final int maxCount
    ) {
        if (weightedSpecials == null || weightedSpecials.isEmpty()) {
            this.weightedSpecials = Collections.emptyMap();
        } else {
            this.weightedSpecials = Collections.unmodifiableMap(new LinkedHashMap<>(weightedSpecials));
        }
        this.minCount = Math.max(0, minCount);
        this.maxCount = Math.max(this.minCount, maxCount);
    }

    public static OvermapSpecialSettings disabled() {
        return new OvermapSpecialSettings(Collections.emptyMap(), 0, 0);
    }

    public boolean isEnabled() {
        return !weightedSpecials.isEmpty() && maxCount > 0;
    }

    public Map<String, Integer> getWeightedSpecials() {
        return weightedSpecials;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public Optional<String> pickWeightedSpecial(final java.util.Random rng) {
        if (!isEnabled() || rng == null) {
            return Optional.empty();
        }
        int total = 0;
        for (final int weight : weightedSpecials.values()) {
            total += Math.max(1, weight);
        }
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = rng.nextInt(total);
        for (final Map.Entry<String, Integer> entry : weightedSpecials.entrySet()) {
            roll -= Math.max(1, entry.getValue());
            if (roll < 0) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.of(weightedSpecials.keySet().iterator().next());
    }
}
