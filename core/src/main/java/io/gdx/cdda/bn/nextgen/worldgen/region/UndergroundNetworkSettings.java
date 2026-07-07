package io.gdx.cdda.bn.nextgen.worldgen.region;

/** Optional subway / rail / sewer carve passes from region JSON (W17f). */
public final class UndergroundNetworkSettings {

    private final boolean subwaysEnabled;
    private final boolean railsEnabled;
    private final boolean sewersEnabled;

    public UndergroundNetworkSettings(
        final boolean subwaysEnabled,
        final boolean railsEnabled,
        final boolean sewersEnabled
    ) {
        this.subwaysEnabled = subwaysEnabled;
        this.railsEnabled = railsEnabled;
        this.sewersEnabled = sewersEnabled;
    }

    public static UndergroundNetworkSettings disabled() {
        return new UndergroundNetworkSettings(false, false, false);
    }

    public boolean isEnabled() {
        return subwaysEnabled || railsEnabled || sewersEnabled;
    }

    public boolean isSubwaysEnabled() {
        return subwaysEnabled;
    }

    public boolean isRailsEnabled() {
        return railsEnabled;
    }

    public boolean isSewersEnabled() {
        return sewersEnabled;
    }
}
