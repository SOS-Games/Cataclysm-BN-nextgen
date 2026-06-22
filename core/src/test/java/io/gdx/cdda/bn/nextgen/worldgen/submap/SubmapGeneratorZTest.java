package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.GameDataLoader;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OmtBuildingBlitter;
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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmapGeneratorZTest {

    private MapgenPreviewService mapgenService;
    private CityBuildingDefinition duplex;
    private io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData gameData;

    @BeforeEach
    void loadFixtures() throws Exception {
        mapgenService = new MapgenPreviewService();
        mapgenService.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        duplex = mapgenService.getCityBuildings().findById("test_duplex").orElseThrow();
        gameData = GameDataLoader.loadMods(
            Collections.singletonList("bn"),
            GameDataLoadOptions.fromRoots(Collections.singletonList(WorldgenTestFixtures.fixtureDataRoot()))
        );
    }

    @Test
    void differentZUseDifferentSubmapCacheKeys() throws Exception {
        final OvermapGrid overmap = new OvermapGrid(4, 4, "open_air");
        overmap.setOmtId(1, 1, "test_room");
        final SubmapCache cache = new SubmapCache(8);

        final VisitResult ground = SubmapGenerator.visit(
            overmap, 1, 1, 0, 42L, cache, mapgenService, null, null
        );
        final VisitResult upper = SubmapGenerator.visit(
            overmap, 1, 1, 1, 42L, cache, mapgenService, null, null
        );

        assertTrue(ground.hasGrid());
        assertTrue(upper.hasGrid());
        assertNotSame(ground.getGrid(), upper.getGrid());
    }

    @Test
    void duplexVolumeBuildsBothFloorsWithVisitSeed() {
        final long worldSeed = 77L;
        final int anchorX = 1;
        final int anchorY = 1;
        final long previewSeed = SubmapSeed.mix(worldSeed, new SubmapKey(worldSeed, anchorX, anchorY, 0));
        final MapgenPreviewService.MapgenBuildingResult built = mapgenService.generateBuilding(
            duplex,
            gameData,
            new io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions().withPreviewSeed(previewSeed)
        );
        assertEquals(2, built.getVolume().floorCount());
    }

    @Test
    void buildingVisitAtRoofZShowsRoofGrid() {
        final OvermapGrid overmap = new OvermapGrid(8, 8, "open_air");
        final int anchorX = 2;
        final int anchorY = 2;
        OmtBuildingBlitter.blitAt(duplex, overmap, anchorX, anchorY, 0, null, new ArrayList<>());
        overmap.setOmtId(anchorX, anchorY, "test_duplex_ground_north");
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(duplex, anchorX, anchorY, PlacementSource.CITY);
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(List.of(record), new ArrayList<>());

        final VisitResult ground = SubmapGenerator.visit(
            overmap, anchorX, anchorY, 0, 88L, null, new VolumeCache(4), index, mapgenService, null, gameData
        );
        final VisitResult roof = SubmapGenerator.visit(
            overmap, anchorX, anchorY, 1, 88L, null, new VolumeCache(4), index, mapgenService, null, gameData
        );

        assertTrue(ground.isBuildingVisit());
        assertTrue(roof.isBuildingVisit());
        assertEquals(0, ground.getVolume().get().getActiveZ());
        assertEquals(1, roof.getVolume().get().getActiveZ());
        assertNotEquals(
            terrainAtCenter(ground.getGrid()),
            terrainAtCenter(roof.getGrid())
        );
    }

    @Test
    void roofOmtIdSelectsRoofFloorOnBuildingVisit() {
        final OvermapGrid overmap = new OvermapGrid(8, 8, "open_air");
        final int anchorX = 1;
        final int anchorY = 1;
        OmtBuildingBlitter.blitAt(duplex, overmap, anchorX, anchorY, 0, null, new ArrayList<>());
        overmap.setOmtId(anchorX, anchorY, "test_duplex_roof_north");
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(duplex, anchorX, anchorY, PlacementSource.CITY);
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(List.of(record), new ArrayList<>());

        final VisitResult visit = SubmapGenerator.visit(
            overmap,
            anchorX,
            anchorY,
            io.gdx.cdda.bn.nextgen.worldgen.visit.ZLevelResolver.visitZForOmt("test_duplex_roof_north"),
            77L,
            null,
            new VolumeCache(4),
            index,
            mapgenService,
            null,
            gameData
        );

        assertTrue(visit.isBuildingVisit());
        assertEquals(2, visit.getVolume().get().floorCount());
        assertEquals("t_wall", visit.getGrid().get(1, 1).getTerrainId());
        assertEquals(1, visit.getVolume().get().getActiveZ());
    }

    private static String terrainAtCenter(final MapGrid grid) {
        final int x = grid.width() / 2;
        final int y = grid.height() / 2;
        return grid.get(x, y).getTerrainId();
    }
}
