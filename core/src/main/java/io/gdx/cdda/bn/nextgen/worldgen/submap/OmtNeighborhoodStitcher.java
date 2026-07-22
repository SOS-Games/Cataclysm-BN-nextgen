package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.mapgen.compose.SpawnMarkerTransform;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.generate.BuildingFootprint;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingIndex;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.visit.VolumeCache;
import io.gdx.cdda.bn.nextgen.worldgen.visit.VolumeCacheKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Blits a neighborhood of OMT submaps into one connected {@link MapGrid} (W3 patch visit). */
public final class OmtNeighborhoodStitcher {

    public static final int DEFAULT_RADIUS = 1;
    /** Default walkaround height in OMT cells (even sizes bias one extra cell south). */
    public static final int DEFAULT_WALKAROUND_HEIGHT = 12;
    /** Default walkaround width in OMT cells (even sizes bias one extra cell east). */
    public static final int DEFAULT_WALKAROUND_WIDTH = 20;
    /**
     * @deprecated Prefer {@link #DEFAULT_WALKAROUND_WIDTH} / {@link #DEFAULT_WALKAROUND_HEIGHT}.
     * Kept as the square side length for older call sites (equals height).
     */
    @Deprecated
    public static final int DEFAULT_WALKAROUND_SIZE = DEFAULT_WALKAROUND_HEIGHT;

    private OmtNeighborhoodStitcher() {}

    public static VisitResult stitch(
        final OvermapGrid overmap,
        final int centerOmtX,
        final int centerOmtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final VolumeCache volumeCache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry,
        final RegionSettingsDefinition region,
        final int radius
    ) {
        final int resolvedRadius = Math.max(0, radius);
        return stitchWindow(
            overmap,
            centerOmtX,
            centerOmtY,
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
            region,
            resolvedRadius,
            resolvedRadius,
            resolvedRadius,
            resolvedRadius
        );
    }

    /**
     * Stitch a square OMT window of {@code size}×{@code size} (clamped at overmap edges).
     * For even sizes the focus sits slightly toward the NW of the window.
     */
    public static VisitResult stitchBySize(
        final OvermapGrid overmap,
        final int centerOmtX,
        final int centerOmtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final VolumeCache volumeCache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry,
        final RegionSettingsDefinition region,
        final int size
    ) {
        return stitchBySize(
            overmap,
            centerOmtX,
            centerOmtY,
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
            region,
            size,
            size
        );
    }

    /**
     * Stitch a rectangular OMT window of {@code width}×{@code height} (clamped at overmap edges).
     * For even sizes the focus sits slightly toward the NW of the window.
     */
    public static VisitResult stitchBySize(
        final OvermapGrid overmap,
        final int centerOmtX,
        final int centerOmtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final VolumeCache volumeCache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry,
        final RegionSettingsDefinition region,
        final int width,
        final int height
    ) {
        final int resolvedW = Math.max(1, width);
        final int resolvedH = Math.max(1, height);
        return stitchWindow(
            overmap,
            centerOmtX,
            centerOmtY,
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
            region,
            (resolvedW - 1) / 2,
            resolvedW / 2,
            (resolvedH - 1) / 2,
            resolvedH / 2
        );
    }

    private static VisitResult stitchWindow(
        final OvermapGrid overmap,
        final int centerOmtX,
        final int centerOmtY,
        final int z,
        final long worldSeed,
        final SubmapCache cache,
        final VolumeCache volumeCache,
        final PlacedBuildingIndex placementIndex,
        final MapgenPreviewService mapgenPreviewService,
        final OvermapTerrainRegistry oterRegistry,
        final LoadedGameData gameData,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry,
        final RegionSettingsDefinition region,
        final int halfLoX,
        final int halfHiX,
        final int halfLoY,
        final int halfHiY
    ) {
        if (overmap == null) {
            return emptyResult("", Collections.singletonList("overmap is required"));
        }
        final int minOmtX = Math.max(0, centerOmtX - halfLoX);
        final int maxOmtX = Math.min(overmap.width() - 1, centerOmtX + halfHiX);
        final int minOmtY = Math.max(0, centerOmtY - halfLoY);
        final int maxOmtY = Math.min(overmap.height() - 1, centerOmtY + halfHiY);
        final int stride = OmtStitchComposer.DEFAULT_OMT_SIZE;
        final int canvasWidth = (maxOmtX - minOmtX + 1) * stride;
        final int canvasHeight = (maxOmtY - minOmtY + 1) * stride;

        final List<String> warnings = new ArrayList<>();
        final Set<Long> coveredOmtCells = new HashSet<>();
        final Set<VolumeCacheKey> blittedBuildings = new HashSet<>();
        final List<SpawnMarker> spawnMarkers = new ArrayList<>();
        boolean allFromCache = true;
        boolean anyContent = false;

        String centerOmtId = "";
        try {
            centerOmtId = overmap.getOmtId(centerOmtX, centerOmtY);
        } catch (final IndexOutOfBoundsException ignored) {
            return emptyResult("", Collections.singletonList("OMT out of bounds"));
        }

        final String fillTer = "t_grass";
        final MapGrid canvas = new MapGrid(canvasWidth, canvasHeight, fillTer);

        for (int omtY = minOmtY; omtY <= maxOmtY; omtY++) {
            for (int omtX = minOmtX; omtX <= maxOmtX; omtX++) {
                final PlacedBuildingIndex index = placementIndex == null
                    ? PlacedBuildingIndex.EMPTY
                    : placementIndex;
                final Optional<PlacedBuildingRecord> placement = index.findAt(omtX, omtY);
                if (!placement.isPresent()) {
                    continue;
                }
                final PlacedBuildingRecord record = placement.get();
                final VolumeCacheKey volumeKey = new VolumeCacheKey(
                    worldSeed,
                    record.getBuildingId(),
                    record.getAnchorX(),
                    record.getAnchorY()
                );
                if (!blittedBuildings.add(volumeKey)) {
                    markFootprintCovered(record, coveredOmtCells);
                    continue;
                }

                final String anchorOmtId = overmap.getOmtId(record.getAnchorX(), record.getAnchorY());
                final VisitResult buildingVisit = SubmapGenerator.visitPlacedBuilding(
                    overmap,
                    record.getAnchorX(),
                    record.getAnchorY(),
                    z,
                    worldSeed,
                    volumeCache,
                    record,
                    mapgenPreviewService,
                    gameData,
                    anchorOmtId,
                    mutableSpecials,
                    connectionRegistry,
                    region
                );
                warnings.addAll(buildingVisit.getWarnings());
                if (!buildingVisit.hasGrid()) {
                    continue;
                }
                anyContent = true;
                allFromCache = allFromCache && buildingVisit.isFromCache();

                final BuildingFootprint footprint = BuildingFootprint.atZ(record.getDefinition(), z);
                final int blitX = (record.getAnchorX() + footprint.getMinOffsetX() - minOmtX) * stride;
                final int blitY = (record.getAnchorY() + footprint.getMinOffsetY() - minOmtY) * stride;
                canvas.blitFrom(buildingVisit.getGrid(), blitX, blitY, fillTer);
                spawnMarkers.addAll(SpawnMarkerTransform.translate(buildingVisit.getSpawnMarkers(), blitX, blitY));
                markFootprintCovered(record, coveredOmtCells);
            }
        }

        for (int omtY = minOmtY; omtY <= maxOmtY; omtY++) {
            for (int omtX = minOmtX; omtX <= maxOmtX; omtX++) {
                if (coveredOmtCells.contains(omtCellKey(omtX, omtY))) {
                    continue;
                }
                final VisitResult cellVisit = SubmapGenerator.visitSingleCell(
                    overmap,
                    omtX,
                    omtY,
                    z,
                    worldSeed,
                    cache,
                    placementIndex,
                    mapgenPreviewService,
                    oterRegistry,
                    gameData,
                    mutableSpecials,
                    connectionRegistry,
                    region
                );
                warnings.addAll(cellVisit.getWarnings());
                if (!cellVisit.hasGrid()) {
                    continue;
                }
                anyContent = true;
                allFromCache = allFromCache && cellVisit.isFromCache();
                final int blitX = (omtX - minOmtX) * stride;
                final int blitY = (omtY - minOmtY) * stride;
                canvas.blitFrom(cellVisit.getGrid(), blitX, blitY, fillTer);
                spawnMarkers.addAll(SpawnMarkerTransform.translate(cellVisit.getSpawnMarkers(), blitX, blitY));
            }
        }

        if (!anyContent) {
            return emptyResult(centerOmtId, warnings);
        }

        final String regionId = region == null ? "default" : region.getId();
        final long previewSeed = SubmapSeed.mix(worldSeed, new SubmapKey(worldSeed, centerOmtX, centerOmtY, z));
        VisitRegionalResolver.applyToGrid(
            canvas,
            mapgenPreviewService.getRegionContext(),
            regionId,
            previewSeed,
            warnings
        );

        return VisitResult.forPatch(
            canvas,
            warnings,
            spawnMarkers,
            allFromCache && anyContent,
            centerOmtId,
            minOmtX,
            minOmtY,
            centerOmtX,
            centerOmtY
        );
    }

    private static void markFootprintCovered(
        final PlacedBuildingRecord record,
        final Set<Long> coveredOmtCells
    ) {
        if (record == null || record.getDefinition() == null) {
            return;
        }
        final BuildingFootprint footprint = BuildingFootprint.atZ(record.getDefinition(), 0);
        for (final CityBuildingPiece piece : footprint.getPieces()) {
            coveredOmtCells.add(omtCellKey(
                record.getAnchorX() + piece.getOffsetX(),
                record.getAnchorY() + piece.getOffsetY()
            ));
        }
    }

    private static long omtCellKey(final int omtX, final int omtY) {
        return ((long) omtX << 32) | (omtY & 0xffffffffL);
    }

    private static VisitResult emptyResult(final String omtId, final List<String> warnings) {
        return new VisitResult(null, warnings, false, omtId);
    }
}
