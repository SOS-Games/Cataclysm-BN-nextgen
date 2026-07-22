package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.worldgen.region.RegionMapExtrasSettings;

import java.util.Random;

/** Rolls region {@code map_extras.road} style events for road visits (R4). */
public final class RoadMapExtras {

    private RoadMapExtras() {}

    public static String roll(final long seed, final RegionMapExtrasSettings settings) {
        final RegionMapExtrasSettings extras = settings == null ? RegionMapExtrasSettings.roadDefaults() : settings;
        if (extras.getChance() <= 0) {
            return null;
        }
        final Random rng = new Random(seed ^ 0x524F4144L);
        if (rng.nextInt(Math.max(1, extras.getChance())) != 0) {
            return null;
        }
        return extras.pickWeighted(rng);
    }
}
