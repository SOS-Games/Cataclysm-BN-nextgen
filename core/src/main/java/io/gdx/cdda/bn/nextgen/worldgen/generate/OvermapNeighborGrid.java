package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory overmap buffer keyed by world overmap coordinates (W16 hydrology neighbor wiring).
 * Row-major batch generation supplies north/west neighbors during carve; a repolish pass reads
 * all four edges once the batch is complete.
 */
public final class OvermapNeighborGrid {

    private final OvermapGenerateOptions baseOptions;
    private final CityBuildingRegistry buildings;
    private final OvermapTerrainRegistry oterRegistry;
    private final OvermapConnectionRegistry connectionRegistry;
    private final MutableSpecialRegistry mutableSpecials;
    private final RegionSettingsRegistry regionSettings;

    private final Map<Long, OvermapGrid> grids = new HashMap<>();
    private final Map<Long, OvermapGenerateResult> results = new HashMap<>();

    public OvermapNeighborGrid(
        final OvermapGenerateOptions baseOptions,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapConnectionRegistry connectionRegistry,
        final MutableSpecialRegistry mutableSpecials,
        final RegionSettingsRegistry regionSettings
    ) {
        if (baseOptions == null) {
            throw new IllegalArgumentException("baseOptions is required");
        }
        this.baseOptions = baseOptions;
        this.buildings = buildings == null ? CityBuildingRegistry.empty() : buildings;
        this.oterRegistry = oterRegistry;
        this.connectionRegistry = connectionRegistry;
        this.mutableSpecials = mutableSpecials == null ? MutableSpecialRegistry.empty() : mutableSpecials;
        this.regionSettings = regionSettings == null ? RegionSettingsRegistry.empty() : regionSettings;
    }

    public void clear() {
        grids.clear();
        results.clear();
    }

    public boolean contains(final int omX, final int omY) {
        return results.containsKey(cellKey(omX, omY));
    }

    public OvermapGrid getGrid(final int omX, final int omY) {
        return grids.get(cellKey(omX, omY));
    }

    public OvermapGenerateResult getResult(final int omX, final int omY) {
        return results.get(cellKey(omX, omY));
    }

    /**
     * Generate one overmap tile, recursively ensuring west and north tiles exist first so edge
     * stitch can read those neighbors (exploration-style expansion).
     */
    public OvermapGenerateResult getOrGenerate(final int omX, final int omY) {
        final long key = cellKey(omX, omY);
        final OvermapGenerateResult cached = results.get(key);
        if (cached != null) {
            return cached;
        }
        if (!contains(omX - 1, omY)) {
            getOrGenerate(omX - 1, omY);
        }
        if (!contains(omX, omY - 1)) {
            getOrGenerate(omX, omY - 1);
        }
        final OvermapGenerateResult result = generateAt(omX, omY, true);
        repolishHydrologyNeighborhood(omX, omY);
        return result;
    }

    /** Generate a rectangular batch in row-major order, then repolish hydrology on all tiles. */
    public List<OvermapGenerateResult> generateBatch(
        final int originOmX,
        final int originOmY,
        final int tilesX,
        final int tilesY
    ) {
        if (tilesX <= 0 || tilesY <= 0) {
            throw new IllegalArgumentException("tilesX and tilesY must be positive");
        }
        final List<OvermapGenerateResult> batch = new ArrayList<>(tilesX * tilesY);
        for (int oy = originOmY; oy < originOmY + tilesY; oy++) {
            for (int ox = originOmX; ox < originOmX + tilesX; ox++) {
                batch.add(generateAt(ox, oy, true));
            }
        }
        repolishBatch(originOmX, originOmY, tilesX, tilesY);
        return batch;
    }

    private OvermapGenerateResult generateAt(final int omX, final int omY, final boolean deferRepolish) {
        final long key = cellKey(omX, omY);
        final OvermapGenerateResult cached = results.get(key);
        if (cached != null) {
            return cached;
        }
        final OvermapNeighborContext neighbors = neighborsFor(omX, omY);
        final OvermapGenerateOptions options = baseOptions
            .withSeed(OvermapWorldSeeds.mix(baseOptions.getSeed(), omX, omY))
            .withOvermapCoord(omX, omY);
        final OvermapGenerateResult result = OvermapGenerator.generate(
            options,
            buildings,
            oterRegistry,
            connectionRegistry,
            mutableSpecials,
            regionSettings,
            neighbors,
            deferRepolish
        );
        results.put(key, result);
        grids.put(key, result.getGrid());
        return result;
    }

    private void repolishBatch(
        final int originOmX,
        final int originOmY,
        final int tilesX,
        final int tilesY
    ) {
        for (int oy = originOmY; oy < originOmY + tilesY; oy++) {
            for (int ox = originOmX; ox < originOmX + tilesX; ox++) {
                repolishAt(ox, oy);
            }
        }
    }

    private void repolishHydrologyNeighborhood(final int omX, final int omY) {
        repolishAt(omX, omY);
        repolishAt(omX - 1, omY);
        repolishAt(omX, omY - 1);
        repolishAt(omX + 1, omY);
        repolishAt(omX, omY + 1);
    }

    private void repolishAt(final int omX, final int omY) {
        final OvermapGenerateResult result = results.get(cellKey(omX, omY));
        if (result == null) {
            return;
        }
        final OvermapGenerateOptions options = baseOptions
            .withSeed(OvermapWorldSeeds.mix(baseOptions.getSeed(), omX, omY))
            .withOvermapCoord(omX, omY);
        RiverPolisher.polishDirectional(
            result.getGrid(),
            options,
            oterRegistry,
            null,
            neighborsFor(omX, omY)
        );
    }

    private OvermapNeighborContext neighborsFor(final int omX, final int omY) {
        return new OvermapNeighborContext(
            grids.get(cellKey(omX, omY - 1)),
            grids.get(cellKey(omX + 1, omY)),
            grids.get(cellKey(omX, omY + 1)),
            grids.get(cellKey(omX - 1, omY))
        );
    }

    private static long cellKey(final int omX, final int omY) {
        return ((long) omX << 32) ^ (omY & 0xFFFFFFFFL);
    }
}
