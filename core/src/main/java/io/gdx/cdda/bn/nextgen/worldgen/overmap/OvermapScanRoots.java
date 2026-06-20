package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** BN content subpaths that may contain {@code overmap_terrain} JSON. */
public final class OvermapScanRoots {

    private OvermapScanRoots() {}

    public static List<String> scanDirs() {
        return Collections.unmodifiableList(Arrays.asList(
            "overmap/overmap_terrain",
            "overmap_terrain"
        ));
    }

    public static List<String> coLocatedMapgenDirs() {
        return Collections.singletonList("overmap_and_mapgen");
    }
}
