package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPickerIndex;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialLoadResult;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialLoader;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialScanOptions;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutableSpecialBuildingConverterTest {

    @Test
    void assemblesMutableLabIntoBundledBuilding() throws Exception {
        final MutableSpecialLoadResult mutableResult = MutableSpecialLoader.load(
            MutableSpecialScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        final MutableSpecialDefinition definition = mutableResult.getRegistry().find("test_mutable_lab").orElseThrow();

        final List<String> warnings = new ArrayList<>();
        final CityBuildingDefinition building = MutableSpecialBuildingConverter.assembleBuilding(
            definition,
            new Random(7L),
            warnings
        ).orElseThrow();

        assertEquals("test_mutable_lab", building.getId());
        assertTrue(building.isWholeOvermapSpecial());
        assertTrue(building.isBundledBuilding());
        assertEquals(2, building.piecesAtZ(0).size());
    }

    @Test
    void mutableSpecialAppearsInPickerIndex() throws Exception {
        final MapgenScanOptions options = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final MapgenCatalog catalog = JsonMapgenLoader.load(options).getCatalog();
        final CityBuildingRegistry buildings = CityBuildingLoader.load(options);
        final MutableSpecialRegistry mutables = MutableSpecialLoader.load(
            MutableSpecialScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();

        final MapgenPickerIndex index = MapgenPickerIndex.build(catalog, buildings, mutables);

        assertTrue(
            index.all().stream().anyMatch(row -> row.isMutableSpecialRow()
                && row.getMutableSpecial().map(special -> "test_mutable_lab".equals(special.getId())).orElse(false)),
            "expected test_mutable_lab mutable row in picker index"
        );
    }
}
