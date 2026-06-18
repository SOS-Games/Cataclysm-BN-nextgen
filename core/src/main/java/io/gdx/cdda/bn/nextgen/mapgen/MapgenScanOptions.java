package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Options for scanning mapgen JSON under BN data roots (P1). */
public final class MapgenScanOptions {

    private final List<Path> dataRoots;
    private final List<String> modIds;
    private final boolean includePaletteTree;
    private final boolean includeMapgenTree;
    private final boolean includeInlinePalettes;

    public MapgenScanOptions(
        final List<Path> dataRoots,
        final List<String> modIds,
        final boolean includePaletteTree,
        final boolean includeMapgenTree,
        final boolean includeInlinePalettes
    ) {
        this.dataRoots = Collections.unmodifiableList(new ArrayList<>(dataRoots));
        this.modIds = modIds == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(modIds));
        this.includePaletteTree = includePaletteTree;
        this.includeMapgenTree = includeMapgenTree;
        this.includeInlinePalettes = includeInlinePalettes;
    }

    public static MapgenScanOptions defaults() {
        return new MapgenScanOptions(
            DataPaths.gameDataRoots(),
            ModConfiguration.activeModIds(),
            true,
            true,
            false
        );
    }

    public static MapgenScanOptions fromDataRoot(final Path dataRoot) {
        return new MapgenScanOptions(
            Collections.singletonList(dataRoot),
            Collections.emptyList(),
            true,
            true,
            false
        );
    }

    public List<Path> getDataRoots() {
        return dataRoots;
    }

    public List<String> getModIds() {
        return modIds;
    }

    public boolean isIncludePaletteTree() {
        return includePaletteTree;
    }

    public boolean isIncludeMapgenTree() {
        return includeMapgenTree;
    }

    public boolean isIncludeInlinePalettes() {
        return includeInlinePalettes;
    }
}
