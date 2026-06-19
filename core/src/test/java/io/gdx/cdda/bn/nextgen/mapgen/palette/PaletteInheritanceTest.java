package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenLoader;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteInheritanceTest {

    @Test
    void childOverridesParentAndInheritsUnmentionedChars() throws Exception {
        final PaletteRegistry registry = loadFixtureRegistry();
        final List<String> warnings = new ArrayList<>();
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(1L);

        final MergedCharMap merged = registry.merge(Collections.singletonList("child_palette"), warnings, options);

        assertTrue(warnings.isEmpty());
        assertEquals("t_rock", merged.terrainForCodePoint('#').orElseThrow());
        assertEquals("t_floor", merged.terrainForCodePoint('.').orElseThrow());
    }

    @Test
    void threeLevelInheritanceChain() {
        final PaletteRegistry registry = new PaletteRegistry();
        registry.put(MapgenPalette.fromResolvedStrings(
            "grandparent",
            Collections.singletonMap((int) '.', "t_floor"),
            Collections.emptyMap()
        ));
        registry.put(new MapgenPalette(
            "middle",
            List.of("grandparent"),
            Collections.singletonMap((int) '#', new JsonReader().parse("\"t_wall\"")),
            Collections.emptyMap(),
            Collections.emptyMap()
        ));
        registry.put(new MapgenPalette(
            "leaf",
            List.of("middle"),
            Collections.singletonMap((int) '#', new JsonReader().parse("\"t_rock\"")),
            Collections.emptyMap(),
            Collections.emptyMap()
        ));

        final MergedCharMap merged = registry.merge(
            Collections.singletonList("leaf"),
            new ArrayList<>(),
            new JsonMapgenRunOptions()
        );

        assertEquals("t_rock", merged.terrainForCodePoint('#').orElseThrow());
        assertEquals("t_floor", merged.terrainForCodePoint('.').orElseThrow());
    }

    @Test
    void inheritanceCycleThrows() {
        final PaletteRegistry registry = new PaletteRegistry();
        registry.put(new MapgenPalette("palette_a", List.of("palette_b"), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
        registry.put(new MapgenPalette("palette_b", List.of("palette_a"), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        assertThrows(
            IllegalStateException.class,
            () -> registry.merge(Collections.singletonList("palette_a"), new ArrayList<>(), new JsonMapgenRunOptions())
        );
    }

    @Test
    void translateRemapsLookupChar() throws Exception {
        final PaletteRegistry registry = loadFixtureRegistry();
        final MergedCharMap merged = registry.merge(
            Collections.singletonList("translate_palette"),
            new ArrayList<>(),
            new JsonMapgenRunOptions()
        );

        assertEquals("t_floor", merged.terrainForCodePoint('A').orElseThrow());
    }

    @Test
    void weightedBacktickStableForSeed() throws Exception {
        final PaletteRegistry registry = loadFixtureRegistry();
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(42L);
        final List<String> warnings = new ArrayList<>();

        final MergedCharMap first = registry.merge(
            Collections.singletonList("outdoor_mix_palette"),
            warnings,
            options
        );
        final MergedCharMap second = registry.merge(
            Collections.singletonList("outdoor_mix_palette"),
            new ArrayList<>(),
            options
        );

        final String picked = first.terrainForCodePoint('`').orElseThrow();
        assertEquals(picked, second.terrainForCodePoint('`').orElseThrow());
        assertTrue(
            picked.equals("t_grass")
                || picked.equals("t_grass_long")
                || picked.equals("t_shrub")
                || picked.equals("t_dirt")
        );
    }

    @Test
    void paletteInheritRoomMapgen() throws Exception {
        final MapgenScanOptions scanOptions = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final PaletteRegistry palettes = PaletteLoader.load(scanOptions).getPalettes();
        final MapgenCatalog catalog = JsonMapgenLoader.load(scanOptions).getCatalog();
        final JsonMapgenDefinition definition = catalog.findByOmTerrain("test_palette_inherit").get(0);

        final MapGrid grid = JsonMapgenRunner.run(definition, palettes, new JsonMapgenRunOptions());

        assertEquals("t_rock", grid.get(0, 0).getTerrainId());
        assertEquals("t_floor", grid.get(1, 1).getTerrainId());
    }

    @Test
    void translatePaletteRoomMapgen() throws Exception {
        final MapgenScanOptions scanOptions = MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot());
        final PaletteRegistry palettes = PaletteLoader.load(scanOptions).getPalettes();
        final MapgenCatalog catalog = JsonMapgenLoader.load(scanOptions).getCatalog();
        final JsonMapgenDefinition definition = catalog.findByOmTerrain("test_translate_palette").get(0);

        final MapGrid grid = JsonMapgenRunner.run(definition, palettes, new JsonMapgenRunOptions());

        assertEquals("t_floor", grid.get(0, 0).getTerrainId());
        assertEquals("t_floor", grid.get(2, 0).getTerrainId());
    }

    private static PaletteRegistry loadFixtureRegistry() throws Exception {
        return PaletteLoader.load(MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot()))
            .getPalettes();
    }
}
