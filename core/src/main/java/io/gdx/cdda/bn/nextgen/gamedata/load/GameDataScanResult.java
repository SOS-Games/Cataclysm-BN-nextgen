package io.gdx.cdda.bn.nextgen.gamedata.load;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Outcome of a G1 scan-only pass over game JSON files. */
public final class GameDataScanResult {

    private final List<Path> jsonFiles;
    private final Map<String, Integer> objectCountByType;
    private final int totalObjects;

    public GameDataScanResult(
        final List<Path> jsonFiles,
        final Map<String, Integer> objectCountByType,
        final int totalObjects
    ) {
        this.jsonFiles = Collections.unmodifiableList(jsonFiles);
        this.objectCountByType = Collections.unmodifiableMap(new HashMap<>(objectCountByType));
        this.totalObjects = totalObjects;
    }

    public List<Path> getJsonFiles() {
        return jsonFiles;
    }

    public Map<String, Integer> getObjectCountByType() {
        return objectCountByType;
    }

    public int getTotalObjects() {
        return totalObjects;
    }

    public int countObjects(final String type) {
        return objectCountByType.getOrDefault(type, 0);
    }
}
