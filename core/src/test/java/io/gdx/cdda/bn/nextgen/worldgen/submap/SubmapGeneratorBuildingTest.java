package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.GameDataLoader;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.generate.CityPlacer;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OmtBuildingBlitter;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingIndex;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacementSource;
import io.gdx.cdda.bn.nextgen.worldgen.visit.VolumeCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmapGeneratorBuildingTest {

    private MapgenPreviewService mapgenService;
    private CityBuildingDefinition multitile;
    private io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData gameData;

    @BeforeEach
    void loadFixtures() throws Exception {
        mapgenService = new MapgenPreviewService();
        mapgenService.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        multitile = mapgenService.getCityBuildings().findById("test_multitile").orElseThrow();
        gameData = GameDataLoader.loadMods(
            Collections.singletonList("bn"),
            GameDataLoadOptions.fromRoots(Collections.singletonList(WorldgenTestFixtures.fixtureDataRoot()))
        );
    }

    @Test
    void visitUsesMapVolumeForPlacedMultitile() {
        final OvermapGrid overmap = new OvermapGrid(8, 8, "open_air");
        final List<PlacedBuildingRecord> placements = new ArrayList<>();
        final boolean placed = CityPlacer.tryPlace(
            multitile,
            overmap,
            null,
            OmtBuildingBlitter.defaultClearableIds(OvermapGenerateOptions.forSize(8, 8), null),
            new Random(3L),
            new ArrayList<>(),
            null,
            placements,
            PlacementSource.CITY
        );
        assertTrue(placed);
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(placements, new ArrayList<>());
        assertEquals(2, index.cellCount());

        final VisitResult result = SubmapGenerator.visit(
            overmap,
            placements.get(0).getAnchorX() + 1,
            placements.get(0).getAnchorY(),
            0,
            77L,
            new SubmapCache(8),
            new VolumeCache(4),
            index,
            mapgenService,
            null,
            gameData
        );

        assertTrue(result.isBuildingVisit());
        assertTrue(result.hasGrid());
        assertTrue(result.getVolume().isPresent());
        assertTrue(result.getGrid().width() > 24, "stitched multitile grid should be wider than one submap");
        assertEquals(multitile.floorCount(), result.getVolume().get().floorCount());
    }

    @Test
    void volumeCacheReusesBuildForSecondCellOfSameBuilding() {
        final OvermapGrid overmap = new OvermapGrid(8, 8, "open_air");
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(multitile, 2, 2, PlacementSource.CITY);
        OmtBuildingBlitter.blitAt(multitile, overmap, 2, 2, 0, null, new ArrayList<>());
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(List.of(record), new ArrayList<>());
        final VolumeCache volumeCache = new VolumeCache(4);

        final VisitResult west = SubmapGenerator.visit(
            overmap, 2, 2, 0, 55L, null, volumeCache, index, mapgenService, null, gameData
        );
        final VisitResult east = SubmapGenerator.visit(
            overmap, 3, 2, 0, 55L, null, volumeCache, index, mapgenService, null, gameData
        );

        assertTrue(west.isBuildingVisit());
        assertTrue(east.isBuildingVisit());
        assertFalse(west.isFromCache());
        assertTrue(east.isFromCache());
        assertEquals(west.getVolume().get(), east.getVolume().get());
    }

    @Test
    void visitMatchesPickerImportGridAtGroundFloor() {
        final OvermapGrid overmap = new OvermapGrid(8, 8, "open_air");
        final int anchorX = 1;
        final int anchorY = 1;
        final long worldSeed = 100L;
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(multitile, anchorX, anchorY, PlacementSource.CITY);
        OmtBuildingBlitter.blitAt(multitile, overmap, anchorX, anchorY, 0, null, new ArrayList<>());
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(List.of(record), new ArrayList<>());

        final VisitResult visit = SubmapGenerator.visit(
            overmap, anchorX, anchorY, 0, worldSeed, null, new VolumeCache(4), index, mapgenService, null, gameData
        );
        final long previewSeed = SubmapSeed.mix(worldSeed, new SubmapKey(worldSeed, anchorX, anchorY, 0));
        final MapgenPreviewService.MapgenBuildingResult picker = mapgenService.generateBuilding(
            multitile,
            gameData,
            new io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions().withPreviewSeed(previewSeed)
        );

        assertEquals(picker.getVolume().getActiveGrid().width(), visit.getGrid().width());
        assertEquals(picker.getVolume().getActiveGrid().height(), visit.getGrid().height());
        assertEquals(
            picker.getVolume().getActiveGrid().get(2, 2).getTerrainId(),
            visit.getGrid().get(2, 2).getTerrainId()
        );
    }
}
