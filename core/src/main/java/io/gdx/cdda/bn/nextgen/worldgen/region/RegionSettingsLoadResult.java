package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link RegionSettingsLoader#load}. */
public final class RegionSettingsLoadResult {

    private final RegionSettingsRegistry registry;
    private final List<String> warnings;

    public RegionSettingsLoadResult(final RegionSettingsRegistry registry, final List<String> warnings) {
        this.registry = registry == null ? RegionSettingsRegistry.empty() : registry;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public RegionSettingsRegistry getRegistry() {
        return registry;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
