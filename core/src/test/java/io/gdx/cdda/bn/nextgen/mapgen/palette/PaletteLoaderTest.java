package io.gdx.cdda.bn.nextgen.mapgen.palette;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenLoadResult;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenTestFixtures;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PaletteLoaderTest {

    @Test
    void loadsMinimalPaletteFromFixture() throws Exception {
        final MapgenLoadResult result = PaletteLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        );
        final PaletteRegistry registry = result.getPalettes();

        assertTrue(registry.contains("minimal"));
        final MapgenPalette minimal = registry.find("minimal").orElseThrow();
        assertEquals("t_floor", minimal.terrainForCodePoint('.').orElseThrow());
        assertEquals("t_wall", minimal.terrainForCodePoint('#').orElseThrow());
        assertEquals("f_chair", minimal.furnitureForCodePoint('H').orElseThrow());
    }

    @Test
    void loadsWeightedPaletteDeterministically() throws Exception {
        final MapgenLoadResult result = PaletteLoader.load(
            MapgenScanOptions.fromDataRoot(MapgenTestFixtures.fixtureDataRoot())
        );
        final MapgenPalette weighted = result.getPalettes().find("weighted_palette").orElseThrow();

        assertEquals("t_door_c", weighted.terrainForCodePoint('+').orElseThrow());
        assertEquals("t_wall_w", weighted.terrainForCodePoint('|').orElseThrow());
        assertEquals("f_indoor_plant", weighted.furnitureForCodePoint('y').orElseThrow());
    }

    @Test
    void integrationLoadsStandardDomesticPaletteFromSiblingBn() throws Exception {
        final Path bnData = Paths.get("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(bnData.toFile().isDirectory(), "Cataclysm-BN/data not found beside nextgen");

        final MapgenLoadResult result = PaletteLoader.load(MapgenScanOptions.fromDataRoot(bnData));
        final MapgenPalette palette = result.getPalettes().find("standard_domestic_palette").orElse(null);
        assertNotNull(palette);
        assertEquals("t_brick_wall", palette.terrainForCodePoint('#').orElseThrow());
        assertEquals("f_sofa", palette.furnitureForCodePoint('H').orElseThrow());
    }
}
