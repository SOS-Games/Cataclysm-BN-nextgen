package io.gdx.cdda.bn.nextgen.worldgen.generate;

import java.util.Random;

/** Simplified BN city size tier for urban blob radius (W17a). */
public enum CityTier {
    TINY(1, false),
    SMALL(2, false),
    NORMAL(3, false),
    LARGE(4, true),
    HUGE(5, true);

    private final int radiusScale;
    private final boolean attemptFinale;

    CityTier(final int radiusScale, final boolean attemptFinale) {
        this.radiusScale = Math.max(1, radiusScale);
        this.attemptFinale = attemptFinale;
    }

    public boolean isAttemptFinale() {
        return attemptFinale;
    }

    public int effectiveRadius(final int baseCitySize) {
        final int base = baseCitySize > 0 ? baseCitySize : 3;
        return Math.max(1, (base * radiusScale + 1) / 2);
    }

    public static CityTier roll(final Random rng, final int baseCitySize) {
        if (rng == null) {
            return NORMAL;
        }
        final int roll = rng.nextInt(6);
        if (roll == 0) {
            return TINY;
        }
        if (roll <= 2) {
            return SMALL;
        }
        if (roll == 3) {
            return NORMAL;
        }
        if (roll == 4) {
            return LARGE;
        }
        return HUGE;
    }
}
