package io.gdx.cdda.bn.nextgen.worldgen.connection;

import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scan options for BN {@code overmap_connection} JSON (W5). */
public final class OvermapConnectionScanOptions {

    private final List<Path> dataRoots;
    private final List<String> modIds;

    public OvermapConnectionScanOptions(final List<Path> dataRoots, final List<String> modIds) {
        this.dataRoots = Collections.unmodifiableList(new ArrayList<>(dataRoots));
        this.modIds = modIds == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(modIds));
    }

    public static OvermapConnectionScanOptions defaults() {
        return new OvermapConnectionScanOptions(
            DataPaths.gameDataRoots(),
            ModConfiguration.activeModIds()
        );
    }

    public static OvermapConnectionScanOptions fromDataRoot(final Path dataRoot) {
        return new OvermapConnectionScanOptions(
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
