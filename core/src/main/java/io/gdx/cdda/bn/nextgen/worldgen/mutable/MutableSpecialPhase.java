package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One assembly phase from mutable special JSON (W6 v1: simple overmap entries only). */
public final class MutableSpecialPhase {

    private final List<MutablePhaseEntry> entries;

    public MutableSpecialPhase(final List<MutablePhaseEntry> entries) {
        this.entries = entries == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<MutablePhaseEntry> getEntries() {
        return entries;
    }
}
