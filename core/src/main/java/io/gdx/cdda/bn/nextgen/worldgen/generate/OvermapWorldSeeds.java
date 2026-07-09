package io.gdx.cdda.bn.nextgen.worldgen.generate;

/** Per-overmap seed mixing (BN overmapbuffer-style coordinate salt). */
public final class OvermapWorldSeeds {

    private OvermapWorldSeeds() {}

    public static long mix(final long worldSeed, final int omX, final int omY) {
        long h = worldSeed;
        h ^= (long) omX * 0x9E3779B97F4A7C15L;
        h ^= (long) omY * 0xC2B2AE3D27D4EB4FL;
        return h;
    }
}
