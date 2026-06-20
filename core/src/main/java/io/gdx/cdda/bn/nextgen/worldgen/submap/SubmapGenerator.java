package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Generates and caches submap grids when visiting an OMT cell (W3). */
public final class SubmapGenerator {

    private SubmapGenerator() {}

    public static VisitResult visit(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData
    ) {
        if (overmap == null || mapgenPreviewService == null || !mapgenPreviewService.isLoaded()) {
            return emptyResult("", Collections.singletonList("mapgen catalog not loaded"));
        }

        final String omtId;
        try {
            omtId = overmap.getOmtId(omtX, omtY);
        } catch (final IndexOutOfBoundsException e) {
            return emptyResult("", Collections.singletonList("OMT out of bounds: (" + omtX + "," + omtY + ")"));
        }

        final SubmapKey key = new SubmapKey(worldSeed, omtX, omtY, z);
        if (cache != null) {
            final Optional<MapGrid> cached = cache.get(key);
            if (cached.isPresent()) {
                return new VisitResult(cached.get(), Collections.emptyList(), true, omtId);
            }
        }

        final List<String> warnings = new ArrayList<>();
        final long previewSeed = SubmapSeed.mix(worldSeed, key);
        final Random pickRng = new Random(previewSeed ^ 0x5EEDL);
        final Optional<JsonMapgenDefinition> definition = MapgenPicker.pick(
            omtId,
            z,
            pickRng,
            oterRegistry,
            mapgenPreviewService.getCatalog(),
            warnings
        );
        if (!definition.isPresent()) {
            return new VisitResult(null, warnings, false, omtId);
        }

        final JsonMapgenRunOptions runOptions = new JsonMapgenRunOptions()
            .withPreviewSeed(previewSeed)
            .withOmtRotation(MapGridRotator.rotationFromOmSuffix(omtId))
            .withNeighborOmtIds(collectNeighborOmtIds(overmap, omtX, omtY));

        final MapgenPreviewService.MapgenPreviewResult generated = mapgenPreviewService.generate(
            definition.get(),
            gameData,
            runOptions
        );
        warnings.addAll(generated.getRunWarnings());

        final MapGrid grid = generated.getGrid();
        if (cache != null && grid != null) {
            cache.put(key, grid);
        }
        return new VisitResult(grid, warnings, false, omtId);
    }

    /** Legacy entry point for direct OMT id visits without grid context. */
    public static Optional<MapgenPreviewService.MapgenPreviewResult> visitByOmtId(
        final String omtId,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final JsonMapgenRunOptions runOptions
    ) {
        if (omtId == null || omtId.isEmpty() || mapgenPreviewService == null || !mapgenPreviewService.isLoaded()) {
            return Optional.empty();
        }
        final List<String> warnings = new ArrayList<>();
        final JsonMapgenRunOptions options = runOptions == null ? new JsonMapgenRunOptions() : runOptions;
        final Optional<JsonMapgenDefinition> definition = MapgenPicker.pick(
            omtId,
            0,
            options.createRng(null),
            oterRegistry,
            mapgenPreviewService.getCatalog(),
            warnings
        );
        if (!definition.isPresent()) {
            return Optional.empty();
        }
        final JsonMapgenRunOptions resolved = options
            .withOmtRotation(MapGridRotator.rotationFromOmSuffix(omtId));
        return Optional.of(mapgenPreviewService.generate(definition.get(), gameData, resolved));
    }

    private static List<String> collectNeighborOmtIds(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY
    ) {
        final List<String> neighbors = new ArrayList<>(4);
        addNeighborIfPresent(overmap, omtX, omtY - 1, neighbors);
        addNeighborIfPresent(overmap, omtX + 1, omtY, neighbors);
        addNeighborIfPresent(overmap, omtX, omtY + 1, neighbors);
        addNeighborIfPresent(overmap, omtX - 1, omtY, neighbors);
        return neighbors;
    }

    private static void addNeighborIfPresent(
        final OvermapGrid overmap,
        final int x,
        final int y,
        final List<String> neighbors
    ) {
        if (x < 0 || y < 0 || x >= overmap.width() || y >= overmap.height()) {
            return;
        }
        neighbors.add(overmap.getOmtId(x, y));
    }

    private static VisitResult emptyResult(final String omtId, final List<String> warnings) {
        return new VisitResult(null, warnings, false, omtId);
    }
}
