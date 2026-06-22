package io.gdx.cdda.bn.nextgen.worldgen.visit;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacementSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZLevelResolverTest {

    private CityBuildingDefinition duplex;

    @BeforeEach
    void loadDuplex() throws Exception {
        final MapgenPreviewService mapgen = new MapgenPreviewService();
        mapgen.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        duplex = mapgen.getCityBuildings().findById("test_duplex").orElseThrow();
    }

    @Test
    void pieceAtCellMatchesRoofOmtOnDuplex() {
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(duplex, 1, 1, PlacementSource.CITY);
        final Optional<io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece> piece = ZLevelResolver.pieceAtCell(
            duplex,
            record,
            1,
            1,
            "test_duplex_roof_north",
            ZLevelResolver.ROOF_Z
        );
        assertTrue(piece.isPresent());
        assertEquals(1, piece.get().getZLevel());
    }

    @Test
    void activeZForVisitSelectsRoofFloor() throws Exception {
        final MapgenPreviewService mapgen = new MapgenPreviewService();
        mapgen.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        final MapVolume volume = mapgen.generateBuilding(duplex, null).getVolume();
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(duplex, 1, 1, PlacementSource.CITY);
        final Optional<io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece> piece = ZLevelResolver.pieceAtCell(
            duplex,
            record,
            1,
            1,
            "test_duplex_roof_north",
            ZLevelResolver.ROOF_Z
        );
        final int activeZ = ZLevelResolver.activeZForVisit(
            volume,
            ZLevelResolver.ROOF_Z,
            "test_duplex_roof_north",
            piece,
            new ArrayList<>()
        );
        assertEquals(1, activeZ);
    }

    @Test
    void infersBasementFromSuffix() {
        assertEquals(-1, ZLevelResolver.inferFromOmtId("house_09_basement_north"));
    }

    @Test
    void infersRoofFromSuffix() {
        assertEquals(ZLevelResolver.ROOF_Z, ZLevelResolver.inferFromOmtId("test_duplex_roof_north"));
    }

    @Test
    void infersGroundFromSuffix() {
        assertEquals(0, ZLevelResolver.inferFromOmtId("test_duplex_ground_north"));
    }

    @Test
    void infersSecondFloorFromSuffix() {
        assertEquals(1, ZLevelResolver.inferFromOmtId("2storyModern01_second"));
    }

    @Test
    void plainOmtHasNoInferredZ() {
        assertFalse(ZLevelResolver.inferFromOmtIdOptional("test_room").isPresent());
    }

    @Test
    void omTerrainMatchesRoofHint() {
        assertTrue(ZLevelResolver.omTerrainMatchesZ("test_duplex_roof", ZLevelResolver.ROOF_Z));
        assertFalse(ZLevelResolver.omTerrainMatchesZ("test_duplex_ground", ZLevelResolver.ROOF_Z));
    }
}
