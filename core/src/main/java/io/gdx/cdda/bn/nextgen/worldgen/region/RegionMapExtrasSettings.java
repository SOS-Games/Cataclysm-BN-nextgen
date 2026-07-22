package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Weighted map-extra pool for one region extras key (e.g. {@code road}). */
public final class RegionMapExtrasSettings {

    private final int chance;
    private final Map<String, Integer> weights;

    public RegionMapExtrasSettings(final int chance, final Map<String, Integer> weights) {
        this.chance = Math.max(0, chance);
        this.weights = weights == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(weights));
    }

    public static RegionMapExtrasSettings disabled() {
        return new RegionMapExtrasSettings(0, Collections.emptyMap());
    }

    /** BN default {@code map_extras.road} subset (chance 75). */
    public static RegionMapExtrasSettings roadDefaults() {
        final Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("mx_roadworks", 100);
        weights.put("mx_roadblock", 100);
        weights.put("mx_casings", 100);
        weights.put("mx_bandits_block", 80);
        weights.put("mx_mayhem", 50);
        weights.put("mx_collegekids", 50);
        weights.put("mx_science", 40);
        weights.put("mx_surrounded_vehicle", 30);
        weights.put("mx_corpses", 30);
        weights.put("mx_military", 25);
        weights.put("mx_drugdeal", 30);
        weights.put("mx_prison_bus", 15);
        weights.put("mx_supplydrop", 10);
        weights.put("mx_crater", 10);
        weights.put("mx_helicopter", 1);
        weights.put("mx_aircraft", 1);
        return new RegionMapExtrasSettings(75, weights);
    }

    public int getChance() {
        return chance;
    }

    public Map<String, Integer> getWeights() {
        return weights;
    }

    public String pickWeighted(final Random rng) {
        if (weights.isEmpty() || rng == null) {
            return null;
        }
        int total = 0;
        for (final int w : weights.values()) {
            if (w > 0) {
                total += w;
            }
        }
        if (total <= 0) {
            return null;
        }
        int roll = rng.nextInt(total);
        for (final Map.Entry<String, Integer> entry : weights.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }
}
