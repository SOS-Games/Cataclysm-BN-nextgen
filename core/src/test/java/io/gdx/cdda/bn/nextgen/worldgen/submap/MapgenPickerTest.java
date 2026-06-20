package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalogResult;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MapgenPickerTest {

    @Test
    void weightedPickFavorsHigherWeightDefinition() throws Exception {
        final MapgenCatalogResult catalogResult = JsonMapgenLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        final List<JsonMapgenDefinition> candidates = catalogResult.getCatalog().findByOmTerrain("test_weighted_pick");
        org.junit.jupiter.api.Assertions.assertEquals(2, candidates.size());

        final Map<String, Integer> counts = new HashMap<>();
        counts.put("AAA", 0);
        counts.put("BBB", 0);

        final Random rng = new Random(42L);
        for (int i = 0; i < 1000; i++) {
            final JsonMapgenDefinition picked = MapgenPicker.pick(
                "test_weighted_pick",
                0,
                rng,
                null,
                catalogResult.getCatalog(),
                new ArrayList<String>()
            ).orElseThrow();
            final String marker = picked.getObjectRoot().get("rows").child.asString();
            counts.put(marker, counts.get(marker) + 1);
        }

        assertTrue(counts.get("BBB") > counts.get("AAA") * 2,
            "expected weight 9 entry to dominate, got AAA=" + counts.get("AAA") + " BBB=" + counts.get("BBB"));
    }
}
