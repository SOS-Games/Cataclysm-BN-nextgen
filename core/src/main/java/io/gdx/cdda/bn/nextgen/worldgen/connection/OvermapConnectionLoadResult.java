package io.gdx.cdda.bn.nextgen.worldgen.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link OvermapConnectionLoader#load}. */
public final class OvermapConnectionLoadResult {

    private final OvermapConnectionRegistry registry;
    private final List<String> warnings;

    public OvermapConnectionLoadResult(
        final OvermapConnectionRegistry registry,
        final List<String> warnings
    ) {
        this.registry = registry == null ? new OvermapConnectionRegistry() : registry;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public OvermapConnectionRegistry getRegistry() {
        return registry;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
