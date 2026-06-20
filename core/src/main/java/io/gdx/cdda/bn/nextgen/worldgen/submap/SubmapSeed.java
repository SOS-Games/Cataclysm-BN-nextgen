package io.gdx.cdda.bn.nextgen.worldgen.submap;

/** Deterministic preview seed mixing for OMT visits (W3). */
public final class SubmapSeed {

    private SubmapSeed() {}

    public static long mix(final long worldSeed, final SubmapKey key) {
        long mixed = worldSeed;
        mixed ^= key.getOmtX() * 73856093L;
        mixed ^= key.getOmtY() * 19349663L;
        mixed ^= key.getZ() * 83492791L;
        return mixed;
    }
}
