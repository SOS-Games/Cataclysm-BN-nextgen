package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scan options for BN mutable overmap special JSON (W6). */
public final class MutableSpecialScanOptions {

    private final List<Path> dataRoots;
    private final List<String> modIds;

    public MutableSpecialScanOptions(final List<Path> dataRoots, final List<String> modIds) {
        this.dataRoots = Collections.unmodifiableList(new ArrayList<>(dataRoots));
        this.modIds = modIds == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(modIds));
    }

    public static MutableSpecialScanOptions defaults() {
        return new MutableSpecialScanOptions(
            DataPaths.gameDataRoots(),
            ModConfiguration.activeModIds()
        );
    }

    public static MutableSpecialScanOptions fromDataRoot(final Path dataRoot) {
        return new MutableSpecialScanOptions(
            Collections.singletonList(dataRoot),
            Collections.emptyList()
        );
    }

    public List<Path> getDataRoots() {
        return dataRoots;
    }

    public List<String> getModIds() {
        return modIds;
    }
}
