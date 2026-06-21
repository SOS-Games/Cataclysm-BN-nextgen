package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapgenVariantPickerTest {

    @Test
    void variantsIncludeAllRunnableOmTerrainEntries() throws Exception {
        final MapgenCatalog catalog = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        ).getCatalog();
        final JsonMapgenDefinition seed = catalog.findByOmTerrain("test_weighted_pick").get(0);
        final List<JsonMapgenDefinition> variants = MapgenVariantPicker.variantsFor(seed, catalog);
        assertEquals(2, variants.size());
    }

    @Test
    void rollVariantRespectsWeights() throws Exception {
        final MapgenCatalog catalog = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        ).getCatalog();
        final JsonMapgenDefinition seed = catalog.findByOmTerrain("test_weighted_pick").get(0);
        final Random rng = new Random(42L);
        int bbb = 0;
        for (int i = 0; i < 500; i++) {
            final JsonMapgenDefinition rolled = MapgenVariantPicker.rollVariant(seed, catalog, rng).orElseThrow();
            final String marker = rolled.getObjectRoot().get("rows").child.asString();
            if ("BBB".equals(marker)) {
                bbb++;
            }
        }
        assertTrue(bbb > 300, "expected weighted roll to favor BBB, got " + bbb);
    }

    @Test
    void disabledEntriesExcludedFromPickerIndex() throws Exception {
        final MapgenCatalog catalog = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        ).getCatalog();
        final MapgenPickerIndex index = MapgenPickerIndex.build(catalog, null);
        final long disabledRows = index.all().stream()
            .filter(row -> row.getDefinition()
                .map(def -> def.getOmTerrain().contains("test_disabled_entry"))
                .orElse(false))
            .count();
        assertEquals(1, disabledRows);
    }
}
