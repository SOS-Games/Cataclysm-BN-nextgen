package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of scanning overmap terrain JSON (W1). */
public final class OvermapTerrainLoadResult {

    private final OvermapTerrainRegistry registry;
    private final List<String> warnings;

    public OvermapTerrainLoadResult(final OvermapTerrainRegistry registry, final List<String> warnings) {
        this.registry = registry;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public OvermapTerrainRegistry getRegistry() {
        return registry;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
