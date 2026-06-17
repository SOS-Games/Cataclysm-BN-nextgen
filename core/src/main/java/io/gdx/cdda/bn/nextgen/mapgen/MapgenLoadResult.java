package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of a mapgen palette scan (P1). */
public final class MapgenLoadResult {

    private final PaletteRegistry palettes;
    private final List<String> warnings;

    public MapgenLoadResult(final PaletteRegistry palettes, final List<String> warnings) {
        this.palettes = palettes;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public PaletteRegistry getPalettes() {
        return palettes;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
