package io.gdx.cdda.bn.nextgen.worldgen.placement;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBuildingIndexTest {

    private CityBuildingDefinition multitile;

    @BeforeEach
    void loadFixtures() throws Exception {
        final MapgenPreviewService mapgen = new MapgenPreviewService();
        mapgen.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        multitile = mapgen.getCityBuildings().findById("test_multitile").orElseThrow();
    }

    @Test
    void indexesBothCellsOfTwoByOneFootprint() {
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(multitile, 3, 4, PlacementSource.CITY);
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(List.of(record), new ArrayList<>());

        final Optional<PlacedBuildingRecord> west = index.findAt(3, 4);
        final Optional<PlacedBuildingRecord> east = index.findAt(4, 4);
        assertTrue(west.isPresent());
        assertTrue(east.isPresent());
        assertEquals(record, west.get());
        assertEquals(record, east.get());
    }

    @Test
    void findAtReturnsEmptyOutsideFootprint() {
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(multitile, 1, 1, PlacementSource.CITY);
        final PlacedBuildingIndex index = PlacedBuildingIndex.fromRecords(List.of(record), new ArrayList<>());

        assertFalse(index.findAt(0, 0).isPresent());
        assertFalse(index.findAt(1, 2).isPresent());
    }

    @Test
    void warnsOnOverlappingPlacements() {
        final PlacedBuildingRecord first = PlacedBuildingRecord.of(multitile, 0, 0, PlacementSource.CITY);
        final PlacedBuildingRecord second = PlacedBuildingRecord.of(multitile, 1, 0, PlacementSource.CITY);
        final List<String> warnings = new ArrayList<>();

        PlacedBuildingIndex.fromRecords(List.of(first, second), warnings);

        assertFalse(warnings.isEmpty());
    }
}
