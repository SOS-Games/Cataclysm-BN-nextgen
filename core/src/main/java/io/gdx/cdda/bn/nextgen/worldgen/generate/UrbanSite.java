package io.gdx.cdda.bn.nextgen.worldgen.generate;

/** One procedurally generated urban blob (W17a, W17b local roads). */
public final class UrbanSite {

    private final int centerX;
    private final int centerY;
    private final int radius;
    private final CityTier tier;

    public UrbanSite(final int centerX, final int centerY, final int radius, final CityTier tier) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = Math.max(1, radius);
        this.tier = tier == null ? CityTier.NORMAL : tier;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int[] center() {
        return new int[] { centerX, centerY };
    }

    public int getRadius() {
        return radius;
    }

    public CityTier getTier() {
        return tier;
    }

    public boolean contains(final int x, final int y) {
        return Math.abs(x - centerX) <= radius && Math.abs(y - centerY) <= radius;
    }
}
