package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link MutableSpecialLoader#load}. */
public final class MutableSpecialLoadResult {

    private final MutableSpecialRegistry registry;
    private final List<String> warnings;

    public MutableSpecialLoadResult(final MutableSpecialRegistry registry, final List<String> warnings) {
        this.registry = registry == null ? new MutableSpecialRegistry() : registry;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public MutableSpecialRegistry getRegistry() {
        return registry;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
