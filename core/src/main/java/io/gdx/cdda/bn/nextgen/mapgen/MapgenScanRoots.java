package io.gdx.cdda.bn.nextgen.mapgen;

import java.util.List;

/** BN mod content subpaths that may contain mapgen JSON. */
public final class MapgenScanRoots {

    private MapgenScanRoots() {}

    public static List<String> mapgenDirs() {
        return List.of("mapgen", "overmap_and_mapgen");
    }
}
