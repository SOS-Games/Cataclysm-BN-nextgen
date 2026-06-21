package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingBundleCopyFromTest {

    @Test
    void copyFromStubProducesSingleClearWarning() throws Exception {
        final MapgenScanOptions options = new MapgenScanOptions(
            Arrays.asList(MapgenTestFixtures.fixtureDataRoot()),
            Arrays.asList("bn", "overlay_mod"),
            true,
            true,
            false
        );
        final CityBuildingRegistry registry = CityBuildingLoader.load(options);

        assertFalse(registry.findById("test_copy_from_stub").isPresent());
        assertEquals(
            1,
            registry.getWarnings().stream()
                .filter(w -> w.equals("bundle test_copy_from_stub skipped: copy-from unresolved"))
                .count()
        );
    }
}
