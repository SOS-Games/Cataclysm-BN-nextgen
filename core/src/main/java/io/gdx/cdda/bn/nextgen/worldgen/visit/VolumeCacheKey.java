package io.gdx.cdda.bn.nextgen.worldgen.visit;

/** Cache key for a built {@link io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume} (W7). */
public final class VolumeCacheKey {

    private final long worldSeed;
    private final String buildingId;
    private final int anchorX;
    private final int anchorY;

    public VolumeCacheKey(
        final long worldSeed,
        final String buildingId,
        final int anchorX,
        final int anchorY
    ) {
        this.worldSeed = worldSeed;
        this.buildingId = buildingId == null ? "" : buildingId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public int getAnchorX() {
        return anchorX;
    }

    public int getAnchorY() {
        return anchorY;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof VolumeCacheKey)) {
            return false;
        }
        final VolumeCacheKey key = (VolumeCacheKey) other;
        return worldSeed == key.worldSeed
            && anchorX == key.anchorX
            && anchorY == key.anchorY
            && buildingId.equals(key.buildingId);
    }

    @Override
    public int hashCode() {
        int hash = (int) (worldSeed ^ (worldSeed >>> 32));
        hash = 31 * hash + buildingId.hashCode();
        hash = 31 * hash + anchorX;
        hash = 31 * hash + anchorY;
        return hash;
    }
}
