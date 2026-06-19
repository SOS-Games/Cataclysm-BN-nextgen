package io.gdx.cdda.bn.nextgen.mapgen.palette;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenLoadResult;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteRegistryMergeTest {

    @Test
    void laterPaletteOverridesSharedChar() throws Exception {
        final MapgenLoadResult loaded = PaletteLoader.load(
            MapgenScanOptions.fromDataRoot(
                MapgenTestFixtures.fixtureDataRoot()
            )
        );
        final PaletteRegistry registry = loaded.getPalettes();
        final List<String> warnings = new ArrayList<>();

        final MergedCharMap merged = registry.merge(Arrays.asList("palette_a", "palette_b"), warnings);

        assertTrue(warnings.isEmpty());
        assertEquals("t_wall_b", merged.terrainForCodePoint('#').orElseThrow());
        assertEquals("t_floor_b", merged.terrainForCodePoint('.').orElseThrow());
        assertEquals("f_sofa", merged.furnitureForCodePoint('H').orElseThrow());
    }

    @Test
    void unknownPaletteIdAddsWarning() {
        final PaletteRegistry registry = new PaletteRegistry();
        registry.put(MapgenPalette.fromResolvedStrings(
            "only",
            java.util.Collections.singletonMap((int) '.', "t_floor"),
            java.util.Collections.emptyMap()
        ));
        final List<String> warnings = new ArrayList<>();

        registry.merge(Arrays.asList("only", "missing_palette"), warnings);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("missing_palette"));
    }
}
