package io.gdx.cdda.bn.nextgen.worldgen.region;

/** Deterministic 0..1 cell noise for overmap base fill (W9). */
public final class RegionTerrainNoise {

    private RegionTerrainNoise() {}

    public static double normalized(final long seed, final int x, final int y) {
        return hashToUnit(seed ^ 0x464F52455354L, x, y);
    }

    /** Forest layer noise (BN {@code om_noise_layer_forest} surrogate; distinct salt from floodplain). */
    public static double forestNormalized(final long seed, final int x, final int y) {
        return hashToUnit(seed ^ 0x464F5245535421L, x * 7 + 11, y * 11 + 23);
    }

    /** Floodplain layer noise (BN {@code om_noise_layer_floodplain} surrogate). */
    public static double floodplainNormalized(final long seed, final int x, final int y) {
        final double raw = hashToUnit(seed ^ 0x464C4F4F44504C41L, x * 3 + 17, y * 5 + 31);
        return raw * raw;
    }

    private static double hashToUnit(final long seed, final int x, final int y) {
        long mixed = seed ^ (x * 73856093L) ^ (y * 19349663L);
        mixed = (mixed ^ (mixed >>> 33)) * 0xff51afd7ed558ccdL;
        mixed = (mixed ^ (mixed >>> 33)) * 0xc4ceb9fe1a85ec53L;
        mixed = mixed ^ (mixed >>> 33);
        return (mixed & 0xffffffffL) / (double) 0x100000000L;
    }
}
