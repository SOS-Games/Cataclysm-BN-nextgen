package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingPieceDebugFormatterTest {

    @Test
    void formatsOmtGridAndStitchedChunks() {
        final List<CityBuildingPiece> pieces = Arrays.asList(
            new CityBuildingPiece(0, 0, 0, "test_mutable_west_north"),
            new CityBuildingPiece(1, 0, 0, "test_mutable_east_north")
        );
        final CityBuildingDefinition building = new CityBuildingDefinition(
            "test_mutable_lab",
            null,
            pieces,
            CityBuildingDefinition.LayoutKind.OVERMAP_SPECIAL_WHOLE
        );
        final MapGrid grid = new MapGrid(48, 24, "t_dirt");
        final Map<Integer, MapGrid> gridsByZ = new LinkedHashMap<>();
        gridsByZ.put(0, grid);
        final List<OmtPieceRect> layouts = Arrays.asList(
            new OmtPieceRect(0, 0, 24, 24, "test_mutable_west_north"),
            new OmtPieceRect(24, 0, 24, 24, "test_mutable_east_north")
        );
        final Map<Integer, List<OmtPieceRect>> layoutsByZ = new LinkedHashMap<>();
        layoutsByZ.put(0, layouts);
        final MapVolume volume = new MapVolume("test_mutable_lab", Arrays.asList(0), gridsByZ, layoutsByZ, 0);

        final String text = BuildingPieceDebugFormatter.format(volume, building);

        assertTrue(text.contains("building: test_mutable_lab"));
        assertTrue(text.contains("test_mutable_west_north"));
        assertTrue(text.contains("test_mutable_east_north"));
        assertTrue(text.contains("\"point\": [0, 0, 0]"));
        assertTrue(text.contains("\"point\": [1, 0, 0]"));
        assertTrue(text.contains("omt grid 2x1"));
        assertTrue(text.contains("stitched chunks"));
        assertTrue(text.contains("origin=(24,0)"));
    }
}
