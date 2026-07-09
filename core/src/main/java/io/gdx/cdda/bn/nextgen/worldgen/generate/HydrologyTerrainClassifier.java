package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

/** River/lake OMT id checks for hydrology passes (hydrology v2). */
public final class HydrologyTerrainClassifier {

    private HydrologyTerrainClassifier() {}

    public static boolean isRiverOmt(
        final String omtId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        if (options != null) {
            if (omtId.equals(options.getRiverCenterId()) || omtId.equals(options.getRiverBankId())) {
                return true;
            }
        }
        if (registry != null) {
            if (registry.find(omtId).map(def -> def.getFlags().contains("RIVER")).orElse(false)) {
                return true;
            }
        }
        return omtId.startsWith("river") || omtId.startsWith("test_river");
    }

    public static boolean isLakeOmt(
        final String omtId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        if (options != null && omtId.equals(options.getLakeId())) {
            return true;
        }
        if (registry != null) {
            if (registry.find(omtId).map(def -> def.getFlags().contains("LAKE")).orElse(false)) {
                return true;
            }
        }
        return omtId.startsWith("lake") || omtId.startsWith("test_lake");
    }

    public static boolean isRiverOrLake(
        final String omtId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        return isRiverOmt(omtId, options, registry) || isLakeOmt(omtId, options, registry);
    }

    /** BN {@code is_ot_match("forest", …, CONTAINS)} — substring match on oter id. */
    public static boolean isForestOmt(final String omtId) {
        return omtId != null && omtId.contains("forest");
    }

    public static boolean isRiverOrLakeAt(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        if (grid == null || x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        return isRiverOrLake(grid.getOmtId(x, y), options, registry);
    }
}
