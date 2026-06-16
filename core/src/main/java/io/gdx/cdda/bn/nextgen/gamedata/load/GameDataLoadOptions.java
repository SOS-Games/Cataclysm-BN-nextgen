package io.gdx.cdda.bn.nextgen.gamedata.load;

import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Options for a game-data scan or load session (G1: scan-only). */
public final class GameDataLoadOptions {

    private static final List<String> DEFAULT_SCAN_SUBDIRS =
        Collections.singletonList("furniture_and_terrain");

    private final List<Path> dataRoots;
    private final List<String> scanSubdirs;

    public GameDataLoadOptions(final List<Path> dataRoots, final List<String> scanSubdirs) {
        this.dataRoots = Collections.unmodifiableList(new ArrayList<>(dataRoots));
        if (scanSubdirs == null || scanSubdirs.isEmpty()) {
            this.scanSubdirs = Collections.emptyList();
        } else {
            this.scanSubdirs = Collections.unmodifiableList(new ArrayList<>(scanSubdirs));
        }
    }

    public static GameDataLoadOptions defaults() {
        return new GameDataLoadOptions(DataPaths.gameDataRoots(), DEFAULT_SCAN_SUBDIRS);
    }

    public static GameDataLoadOptions fromRoots(final List<Path> dataRoots) {
        return new GameDataLoadOptions(dataRoots, DEFAULT_SCAN_SUBDIRS);
    }

    public List<Path> getDataRoots() {
        return dataRoots;
    }

    public List<String> getScanSubdirs() {
        return scanSubdirs;
    }
}
