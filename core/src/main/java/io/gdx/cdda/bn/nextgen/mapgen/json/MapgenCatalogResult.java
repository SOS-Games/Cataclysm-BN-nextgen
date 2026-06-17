package io.gdx.cdda.bn.nextgen.mapgen.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of scanning {@code mapgen/} JSON (P2). */
public final class MapgenCatalogResult {

    private final MapgenCatalog catalog;
    private final List<String> warnings;

    public MapgenCatalogResult(final MapgenCatalog catalog, final List<String> warnings) {
        this.catalog = catalog;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public MapgenCatalog getCatalog() {
        return catalog;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
