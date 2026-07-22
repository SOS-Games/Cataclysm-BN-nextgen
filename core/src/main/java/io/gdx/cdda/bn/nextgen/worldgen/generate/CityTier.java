package io.gdx.cdda.bn.nextgen.worldgen.generate;

import java.util.Random;

/** BN city size tier → street length / town radius (W17a / C1). */
public enum CityTier {
    TINY(false),
    SMALL(false),
    NORMAL(false),
    LARGE(true),
    HUGE(true);

    private final boolean attemptFinale;

    CityTier(final boolean attemptFinale) {
        this.attemptFinale = attemptFinale;
    }

    public boolean isAttemptFinale() {
        return attemptFinale;
    }

    /** BN size transforms after rolling tiny/small/large/huge. */
    public int effectiveRadius(final int baseCitySize) {
        final int base = baseCitySize > 0 ? baseCitySize : 3;
        switch (this) {
            case TINY:
                return 1;
            case SMALL:
                return Math.max(1, (base * 2) / 3);
            case LARGE:
                return Math.max(1, (base * 3) / 2);
            case HUGE:
                return Math.max(1, base * 2);
            case NORMAL:
            default:
                return base;
        }
    }

    public static CityTier roll(final Random rng, final int baseCitySize) {
        if (rng == null || baseCitySize < 0) {
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
