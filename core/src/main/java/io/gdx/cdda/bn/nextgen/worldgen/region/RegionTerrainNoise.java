package io.gdx.cdda.bn.nextgen.worldgen.region;

/** Deterministic 0..1 cell noise for overmap base fill (W9). */
public final class RegionTerrainNoise {

    private RegionTerrainNoise() {}

    public static double normalized(final long seed, final int x, final int y) {
        long mixed = seed ^ (x * 73856093L) ^ (y * 19349663L);
        mixed = (mixed ^ (mixed >>> 33)) * 0xff51afd7ed558ccdL;
        mixed = (mixed ^ (mixed >>> 33)) * 0xc4ceb9fe1a85ec53L;
        mixed = mixed ^ (mixed >>> 33);
        return (mixed & 0xffffffffL) / (double) 0x100000000L;
    }
}
