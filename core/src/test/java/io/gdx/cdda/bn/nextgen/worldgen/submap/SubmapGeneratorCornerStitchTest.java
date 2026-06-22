package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.GameDataLoader;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.compose.BuildingPlacementContext;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtPieceRect;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OmtBuildingBlitter;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingIndex;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacementSource;
import io.gdx.cdda.bn.nextgen.worldgen.visit.VolumeCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmapGeneratorCornerStitchTest {

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
    void westCornerMatchesPickerImport() {
        assertCornerMatchesPicker(1, 1, "test_multitile_west_north", 0);
    }

    @Test
    void eastCornerMatchesPickerImport() {
        assertCornerMatchesPicker(2, 1, "test_multitile_east_north", 1);
    }

    private void assertCornerMatchesPicker(
        final int visitX,
        final int visitY,
        final String expectedOvermapId,
        final int expectedPieceIndex
    ) {
        final int anchorX = 1;
        final int anchorY = 1;
        final long worldSeed = 100L;
        final OvermapGrid overmap = new OvermapGrid(8, 8, "open_air");
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(multitile, anchorX, anchorY, PlacementSource.CITY);
        OmtBuildingBlitter.blitAt(multitile, overmap, anchorX, anchorY, 0, null, new java.util.ArrayList<>());
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(List.of(record), new java.util.ArrayList<>());

        final VisitResult visit = SubmapGenerator.visit(
            overmap,
            visitX,
            visitY,
            0,
            worldSeed,
            null,
            new VolumeCache(4),
            index,
            mapgenService,
            null,
            gameData
        );

        final long previewSeed = SubmapSeed.mix(worldSeed, new SubmapKey(worldSeed, anchorX, anchorY, 0));
        final BuildingPlacementContext placementContext = new BuildingPlacementContext(overmap, record, null, null);
        final MapgenPreviewService.MapgenBuildingResult picker = mapgenService.generateBuilding(
            multitile,
            gameData,
            new JsonMapgenRunOptions().withPreviewSeed(previewSeed),
            placementContext
        );

        assertTrue(visit.hasGrid());
        final MapVolume volume = picker.getVolume();
        final List<OmtPieceRect> layouts = volume.getPieceLayoutsAtZ(0);
        assertEquals(2, layouts.size());
        assertEquals(expectedOvermapId, layouts.get(expectedPieceIndex).getOvermapId());

        assertPieceRegionMatches(visit.getGrid(), volume.getGridAtZ(0), layouts.get(expectedPieceIndex));
    }

    private static void assertPieceRegionMatches(
        final MapGrid visitGrid,
        final MapGrid pickerGrid,
        final OmtPieceRect piece
    ) {
        for (int y = 0; y < piece.getHeight(); y++) {
            for (int x = 0; x < piece.getWidth(); x++) {
                final int gridX = piece.getOriginX() + x;
                final int gridY = piece.getOriginY() + y;
                assertEquals(
                    pickerGrid.get(gridX, gridY).getTerrainId(),
                    visitGrid.get(gridX, gridY).getTerrainId(),
                    "terrain at " + gridX + "," + gridY + " for " + piece.getOvermapId()
                );
                assertEquals(
                    pickerGrid.get(gridX, gridY).getFurnitureId(),
                    visitGrid.get(gridX, gridY).getFurnitureId(),
                    "furniture at " + gridX + "," + gridY + " for " + piece.getOvermapId()
                );
            }
        }
    }
}
