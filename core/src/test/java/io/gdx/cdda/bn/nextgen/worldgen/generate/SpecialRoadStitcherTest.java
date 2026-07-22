package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.BuildingBundleScanner;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionLoader;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialRoadStitcherTest {

    private OvermapGrid grid;
    private OvermapConnectionRegistry connections;
    private OvermapTerrainRegistry oterRegistry;
    private OvermapGenerateOptions options;
    private CityBuildingDefinition special;

    @BeforeEach
    void setUp() throws Exception {
        oterRegistry = OvermapTerrainLoader.load(
            OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        connections = OvermapConnectionLoader.load(
            OvermapConnectionScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
        final CityBuildingRegistry buildings = BuildingBundleScanner.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        special = buildings.findById("test_special_wide").orElseThrow();
        grid = new OvermapGrid(24, 24, "test_field");
        options = OvermapGenerateOptions.forSize(24, 24)
            .withTerrainIds("test_field", "test_field")
            .withConnectivity(true, true, "local_road", "test_river", "test_river");
        BaseTerrainFiller.fill(grid, options, null, oterRegistry, new java.util.Random(1L));
    }

    @Test
    void carvesStubOrLinkFromSpecialConnection() {
        assertTrue(!special.getConnections().isEmpty(), "fixture special should declare connections");
        // Place building at (10,10); connection at (0,-1) relative → (10,9).
        OmtBuildingBlitter.blitAt(special, grid, 10, 10, 0, oterRegistry, new ArrayList<>());
        // Existing highway east of connection so stitch prefers a link.
        for (int x = 14; x <= 18; x++) {
            grid.setOmtId(x, 9, "test_road_ew");
        }

        final int painted = SpecialRoadStitcher.stitch(
            grid, special, 10, 10, 0, connections, options, oterRegistry
        );
        assertTrue(painted >= 1, "expected connection carve, got " + painted);
        final String atStub = grid.getOmtId(10, 9);
        assertTrue(
            atStub != null && atStub.startsWith("test_road"),
            "connection cell should become road, got " + atStub
        );
    }
}
