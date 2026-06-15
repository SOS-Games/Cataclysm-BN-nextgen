package io.gdx.cdda.bn.nextgen.tileset.mod;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One registered {@code mod_tileset} entry (unit 04f phase 1). */
public final class ModTilesetEntry {

    private final Path basePath;
    private final Path fullPath;
    private final int numInFile;
    private final List<String> compatibility;

    public ModTilesetEntry(
        final Path basePath,
        final Path fullPath,
        final int numInFile,
        final List<String> compatibility
    ) {
        this.basePath = basePath;
        this.fullPath = fullPath;
        this.numInFile = numInFile;
        this.compatibility = Collections.unmodifiableList(new ArrayList<>(compatibility));
    }

    public Path getBasePath() {
        return basePath;
    }

    public Path getFullPath() {
        return fullPath;
    }

    public int getNumInFile() {
        return numInFile;
    }

    public List<String> getCompatibility() {
        return compatibility;
    }

    public boolean isCompatible(final String tilesetId) {
        return compatibility.contains(tilesetId);
    }
}
