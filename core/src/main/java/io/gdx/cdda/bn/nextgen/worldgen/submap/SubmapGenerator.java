package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.compose.BuildingPlacementContext;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingIndex;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.JoinContext;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.visit.VolumeCache;
import io.gdx.cdda.bn.nextgen.worldgen.visit.VolumeCacheKey;
import io.gdx.cdda.bn.nextgen.worldgen.visit.ZLevelResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        return visit(
            overmap,
            omtX,
            omtY,
            z,
            worldSeed,
            cache,
            null,
            PlacedBuildingIndex.EMPTY,
            mapgenPreviewService,
            oterRegistry,
            gameData
        );
    }

    public static VisitResult visit(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final VolumeCache volumeCache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData
    ) {
        return visit(
            overmap,
            omtX,
            omtY,
            z,
            worldSeed,
            cache,
            volumeCache,
            placementIndex,
            mapgenPreviewService,
            oterRegistry,
            gameData,
            null
        );
    }

    public static VisitResult visit(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final VolumeCache volumeCache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final MutableSpecialRegistry mutableSpecials
    ) {
        return visit(
            overmap,
            omtX,
            omtY,
            z,
            worldSeed,
            cache,
            volumeCache,
            placementIndex,
            mapgenPreviewService,
            oterRegistry,
            gameData,
            mutableSpecials,
            null
        );
    }

    public static VisitResult visit(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final VolumeCache volumeCache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry
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

        final PlacedBuildingIndex index = placementIndex == null ? PlacedBuildingIndex.EMPTY : placementIndex;
        final Optional<PlacedBuildingRecord> placement = index.findAt(omtX, omtY);
        if (placement.isPresent()) {
            final VisitResult buildingVisit = visitPlacedBuilding(
                overmap,
                omtX,
                omtY,
                z,
                worldSeed,
                volumeCache,
                placement.get(),
                mapgenPreviewService,
                gameData,
                omtId,
                mutableSpecials,
                connectionRegistry
            );
            if (buildingVisit.hasGrid()) {
                return buildingVisit;
            }
        }

        return OmtNeighborhoodStitcher.stitch(
            overmap,
            omtX,
            omtY,
            z,
            worldSeed,
            cache,
            volumeCache,
            placementIndex,
            mapgenPreviewService,
            oterRegistry,
            gameData,
            mutableSpecials,
            connectionRegistry,
            OmtNeighborhoodStitcher.DEFAULT_RADIUS
        );
    }

    static VisitResult visitSingleCell(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry
    ) {
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
            final Optional<MapGrid> background = BackgroundOmtSubmapBuilder.buildIfSupported(
                overmap,
                omtX,
                omtY,
                omtId,
                previewSeed,
                connectionRegistry,
                oterRegistry
            );
            if (background.isPresent()) {
                final MapGrid grid = background.get();
                if (cache != null) {
                    cache.put(key, grid);
                }
                return new VisitResult(grid, warnings, false, omtId);
            }
            return new VisitResult(null, warnings, false, omtId);
        }

        final Optional<PlacedBuildingRecord> placement = placementIndex == null
            ? Optional.empty()
            : placementIndex.findAt(omtX, omtY);
        final JoinContext joinContext = resolveJoinContext(
            overmap,
            omtX,
            omtY,
            placement,
            mutableSpecials,
            connectionRegistry
        );
        final JsonMapgenRunOptions runOptions = new JsonMapgenRunOptions()
            .withPreviewSeed(previewSeed)
            .withOmtRotation(MapGridRotator.rotationFromOmSuffix(omtId))
            .withNeighborsByDirection(joinContext.getNeighborsByDirection())
            .withActiveJoins(joinContext.getActiveJoins())
            .withConnectionsByDirection(joinContext.getConnectionsByDirection());

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
        return new VisitResult(grid, warnings, generated.getSpawnMarkers(), false, omtId);
    }

    static VisitResult visitPlacedBuilding(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final int z,
        final long worldSeed,
        final VolumeCache volumeCache,
        final PlacedBuildingRecord record,
        final MapgenPreviewService mapgenPreviewService,
        final LoadedGameData gameData,
        final String omtId,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        final CityBuildingDefinition building = record.getDefinition();
        if (building == null) {
            return emptyResult(omtId, Collections.singletonList("placed building has no definition"));
        }

        final VolumeCacheKey volumeKey = new VolumeCacheKey(
            worldSeed,
            record.getBuildingId(),
            record.getAnchorX(),
            record.getAnchorY()
        );
        final boolean cached = volumeCache != null && volumeCache.contains(volumeKey);
        final List<String> warnings = new ArrayList<>();

        final MapgenPreviewService.MapgenBuildingResult built;
        try {
            if (volumeCache != null) {
                built = volumeCache.getOrBuild(volumeKey, () -> generateBuildingVolume(
                    building,
                    worldSeed,
                    record,
                    overmap,
                    mapgenPreviewService,
                    gameData,
                    warnings,
                    mutableSpecials,
                    connectionRegistry
                ));
            } else {
                built = generateBuildingVolume(
                    building,
                    worldSeed,
                    record,
                    overmap,
                    mapgenPreviewService,
                    gameData,
                    warnings,
                    mutableSpecials,
                    connectionRegistry
                );
            }
        } catch (final RuntimeException e) {
            warnings.add("building visit failed for " + record.getBuildingId() + ": " + e.getMessage());
            return new VisitResult(null, warnings, false, omtId);
        }

        warnings.addAll(built.getRunWarnings());
        final MapVolume volume = built.getVolume();
        if (volume == null) {
            warnings.add("building visit produced no grid for " + record.getBuildingId());
            return new VisitResult(null, warnings, false, omtId);
        }

        final Optional<CityBuildingPiece> pieceAtCell = ZLevelResolver.pieceAtCell(
            building,
            record,
            omtX,
            omtY,
            omtId,
            z
        );
        final int activeZ = ZLevelResolver.activeZForVisit(volume, z, omtId, pieceAtCell, warnings);
        volume.setActiveZ(activeZ);
        final MapGrid activeGrid = volume.getGridAtZ(activeZ);
        if (activeGrid == null) {
            warnings.add("building visit produced no grid for " + record.getBuildingId() + " at z=" + activeZ);
            return new VisitResult(null, warnings, false, omtId);
        }

        return VisitResult.forBuilding(
            activeGrid,
            warnings,
            volume,
            building,
            built.getSpawnMarkersByZ(),
            cached,
            omtId,
            omtX,
            omtY
        );
    }

    private static MapgenPreviewService.MapgenBuildingResult generateBuildingVolume(
        final CityBuildingDefinition building,
        final long worldSeed,
        final PlacedBuildingRecord record,
        final OvermapGrid overmap,
        final MapgenPreviewService mapgenPreviewService,
        final LoadedGameData gameData,
        final List<String> warnings,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        final BuildingPlacementContext placementContext = new BuildingPlacementContext(
            overmap,
            record,
            mutableSpecials,
            connectionRegistry
        );
        final SubmapKey anchorKey = new SubmapKey(worldSeed, record.getAnchorX(), record.getAnchorY(), 0);
        final long previewSeed = SubmapSeed.mix(worldSeed, anchorKey);
        final JsonMapgenRunOptions runOptions = new JsonMapgenRunOptions().withPreviewSeed(previewSeed);
        return mapgenPreviewService.generateBuilding(building, gameData, runOptions, placementContext);
    }

    private static JoinContext resolveJoinContext(
        final OvermapGrid overmap,
        final int omtX,
        final int omtY,
        final Optional<PlacedBuildingRecord> placement,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        if (placement != null && placement.isPresent() && mutableSpecials != null) {
            final PlacedBuildingRecord record = placement.get();
            final Optional<MutableSpecialDefinition> definition = mutableSpecials.find(record.getBuildingId());
            if (definition.isPresent()) {
                return JoinContext.fromPlacement(
                    overmap,
                    record,
                    omtX,
                    omtY,
                    definition.get(),
                    connectionRegistry
                );
            }
        }
        return JoinContext.fromOvermap(overmap, omtX, omtY, connectionRegistry);
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

    private static VisitResult emptyResult(final String omtId, final List<String> warnings) {
        return new VisitResult(null, warnings, false, omtId);
    }
}
