package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scan options for BN overmap terrain JSON (W1). */
public final class OvermapTerrainScanOptions {

    private final List<Path> dataRoots;
    private final List<String> modIds;

    public OvermapTerrainScanOptions(final List<Path> dataRoots, final List<String> modIds) {
        this.dataRoots = Collections.unmodifiableList(new ArrayList<>(dataRoots));
        this.modIds = modIds == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(modIds));
    }

    public static OvermapTerrainScanOptions defaults() {
        return new OvermapTerrainScanOptions(
            DataPaths.gameDataRoots(),
            ModConfiguration.activeModIds()
        );
    }

    public static OvermapTerrainScanOptions fromDataRoot(final Path dataRoot) {
        return new OvermapTerrainScanOptions(
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
