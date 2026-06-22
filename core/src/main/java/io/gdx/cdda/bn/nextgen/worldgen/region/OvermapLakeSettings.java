package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed {@code overmap_lake_settings} from region_settings (W11b). */
public final class OvermapLakeSettings {

    private static final OvermapLakeSettings DISABLED = new OvermapLakeSettings(
        false,
        0.25,
        20,
        "lake",
        Collections.<String>emptyList()
    );

    private final boolean enabled;
    private final double noiseThresholdLake;
    private final int lakeSizeMin;
    private final String lakeOterId;
    private final List<String> shoreExtendableTerrains;

    public OvermapLakeSettings(
        final boolean enabled,
        final double noiseThresholdLake,
        final int lakeSizeMin,
        final String lakeOterId,
        final List<String> shoreExtendableTerrains
    ) {
        this.enabled = enabled;
        this.noiseThresholdLake = noiseThresholdLake;
        this.lakeSizeMin = Math.max(1, lakeSizeMin);
        this.lakeOterId = lakeOterId == null || lakeOterId.isEmpty() ? "lake" : lakeOterId;
        if (shoreExtendableTerrains == null || shoreExtendableTerrains.isEmpty()) {
            this.shoreExtendableTerrains = Collections.emptyList();
        } else {
            this.shoreExtendableTerrains = Collections.unmodifiableList(new ArrayList<>(shoreExtendableTerrains));
        }
    }

    public static OvermapLakeSettings disabled() {
        return DISABLED;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getNoiseThresholdLake() {
        return noiseThresholdLake;
    }

    public int getLakeSizeMin() {
        return lakeSizeMin;
    }

    public String getLakeOterId() {
        return lakeOterId;
    }

    public List<String> getShoreExtendableTerrains() {
        return shoreExtendableTerrains;
    }
}
