package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.map.MapGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of visiting one OMT cell (W3). */
public final class VisitResult {

    private final MapGrid grid;
    private final List<String> warnings;
    private final boolean fromCache;
    private final String omtId;

    public VisitResult(
        final MapGrid grid,
        final List<String> warnings,
        final boolean fromCache,
        final String omtId
    ) {
        this.grid = grid;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
        this.fromCache = fromCache;
        this.omtId = omtId == null ? "" : omtId;
    }

    public MapGrid getGrid() {
        return grid;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public String getOmtId() {
        return omtId;
    }

    public boolean hasGrid() {
        return grid != null;
    }
}
