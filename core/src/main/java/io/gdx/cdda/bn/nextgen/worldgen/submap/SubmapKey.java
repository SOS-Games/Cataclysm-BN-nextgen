package io.gdx.cdda.bn.nextgen.worldgen.submap;

/** Cache key for a visited submap tile (W3). */
public final class SubmapKey {

    private final long worldSeed;
    private final int omtX;
    private final int omtY;
    private final int z;

    public SubmapKey(final long worldSeed, final int omtX, final int omtY, final int z) {
        this.worldSeed = worldSeed;
        this.omtX = omtX;
        this.omtY = omtY;
        this.z = z;
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public int getOmtX() {
        return omtX;
    }

    public int getOmtY() {
        return omtY;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof SubmapKey)) {
            return false;
        }
        final SubmapKey key = (SubmapKey) other;
        return worldSeed == key.worldSeed
            && omtX == key.omtX
            && omtY == key.omtY
            && z == key.z;
    }

    @Override
    public int hashCode() {
        int hash = (int) (worldSeed ^ (worldSeed >>> 32));
        hash = 31 * hash + omtX;
        hash = 31 * hash + omtY;
        hash = 31 * hash + z;
        return hash;
    }
}
