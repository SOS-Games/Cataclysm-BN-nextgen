package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalogResult;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapgenPickerZTest {

    @Test
    void picksGroundMapgenForGroundOmt() throws Exception {
        final MapgenCatalogResult catalogResult = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        final JsonMapgenDefinition picked = MapgenPicker.pick(
            "test_duplex_ground_north",
            0,
            new Random(1L),
            null,
            catalogResult.getCatalog(),
            new ArrayList<>()
        ).orElseThrow();

        assertEquals("t_floor", picked.getObjectRoot().get("fill_ter").asString());
    }

    @Test
    void picksRoofMapgenForRoofOmt() throws Exception {
        final MapgenCatalogResult catalogResult = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        final JsonMapgenDefinition picked = MapgenPicker.pick(
            "test_duplex_roof_north",
            0,
            new Random(1L),
            null,
            catalogResult.getCatalog(),
            new ArrayList<>()
        ).orElseThrow();

        assertEquals("t_wall", picked.getObjectRoot().get("fill_ter").asString());
    }

    @Test
    void doesNotEmitLegacyZStubWarning() throws Exception {
        final MapgenCatalogResult catalogResult = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        final ArrayList<String> warnings = new ArrayList<>();
        MapgenPicker.pick(
            "test_room",
            -1,
            new Random(1L),
            null,
            catalogResult.getCatalog(),
            warnings
        );

        assertFalse(
            warnings.stream().anyMatch(w -> w.contains("not fully supported")),
            "warnings: " + warnings
        );
    }
}
